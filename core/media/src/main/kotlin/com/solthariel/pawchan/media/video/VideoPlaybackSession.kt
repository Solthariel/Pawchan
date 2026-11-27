package com.solthariel.pawchan.media.video

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class VideoPlaybackSession(
    val engine: VideoEngineBackend,
    private val scope: CoroutineScope,
    val url: String,
    private val soundControl: VideoSound = VideoSound(engine),
    private val ownsScope: Boolean = false,
) : VideoPlaybackControls,
    VideoSoundControls by soundControl,
    AutoCloseable {
    internal var audio: SessionAudioFocus? = null
    val privacy = VideoSessionPrivacy(scope, ::pause)
    private val mutableStatus = MutableStateFlow(VideoPlaybackStatus())
    override val status: StateFlow<VideoPlaybackStatus> = mutableStatus

    /** Сигнал первого кадра на текущей поверхности, а не признак открытия файла. */
    val mutableVideoOutput = MutableStateFlow(false)
    val hasVideoOutput: StateFlow<Boolean> = mutableVideoOutput
    private var pendingSeek: Long? = null
    private var requestedPlaying = true
    private var canAutoReplay = false
    internal val wantsPlayback: Boolean get() = requestedPlaying && !closed && !status.value.ended
    private var closed = false

    /** The PiP window holds this session: the viewer must not pause, close or re-attach it. */
    @Volatile var inPip = false

    private val opening =
        VideoSessionOpening(
            scope,
            engine,
            url,
            object : VideoOpenHost {
                override val requestedPlay: Boolean get() = requestedPlaying

                override fun applySound() = soundControl.apply()

                override fun keepPosition() {
                    pendingSeek = pendingSeek ?: status.value.position
                }

                override fun opening(attempt: VideoAttempt) {
                    canAutoReplay = false
                    soundControl.hasAudio = false
                    mutableVideoOutput.value = false
                    mutableStatus.value =
                        status.value.copy(
                            stage = VideoStage.BUFFERING,
                            playing = false,
                            seekable = false,
                            attempt = attempt,
                            ended = false,
                        )
                    audio?.sync()
                }

                override fun received(
                    type: VideoEvent,
                    snapshot: VideoPlaybackStatus,
                ) = onEvent(type, snapshot)

                override fun terminal(attempt: VideoAttempt) {
                    mutableStatus.value =
                        status.value.copy(stage = VideoStage.ERROR, playing = false, ended = false, attempt = attempt)
                    audio?.sync()
                }
            },
        )

    /** Запуск с заданной позиции — например, после возврата из «картинки в картинке». */
    fun startAt(
        position: Long,
        playing: Boolean = true,
    ) {
        pendingSeek = position.takeIf { it > 0L }
        requestedPlaying = playing
        start()
    }

    fun start() = opening.start()

    private fun onEvent(
        type: VideoEvent,
        snapshot: VideoPlaybackStatus,
    ) {
        if (closed) return
        soundControl.hasAudio = engine.metadata.containsKey("audio_format")
        if (type == VideoEvent.ENDED || type == VideoEvent.INCOMPLETE) {
            canAutoReplay = type == VideoEvent.ENDED && requestedPlaying
            requestedPlaying = false
        }
        when (type) {
            VideoEvent.OPENING, VideoEvent.VIDEO_OUTPUT_PENDING -> mutableVideoOutput.value = false
            VideoEvent.VIDEO_OUTPUT -> mutableVideoOutput.value = true
            else -> Unit
        }
        val stage = if (type == VideoEvent.OPENING) VideoStage.BUFFERING else snapshot.stage
        mutableStatus.value =
            snapshot.copy(
                stage = stage,
                attempt = status.value.attempt,
                ended = type == VideoEvent.ENDED || snapshot.ended,
            )
        if (snapshot.seekable) {
            pendingSeek?.let {
                pendingSeek = null
                engine.seekTo(it.coerceIn(0L, snapshot.duration.coerceAtLeast(0L)))
            }
        }
        if (type == VideoEvent.PLAYING && !requestedPlaying) engine.pause()
        audio?.sync()
    }

    override fun retry() {
        if (closed) return
        pendingSeek = pendingSeek ?: status.value.position
        start()
    }

    override fun setSoftware(enabled: Boolean) = opening.selectDecoder(enabled, status.value.hardwareDecoded)

    override fun togglePlay() {
        if (closed) return
        if (status.value.ended) {
            replayFromEnd()
            return
        }
        requestedPlaying = status.value.stage == VideoStage.ERROR || !requestedPlaying
        if (requestedPlaying) {
            if (status.value.stage == VideoStage.ERROR) {
                retry()
            } else {
                engine.play()
            }
        } else {
            pause()
        }
    }

    fun replayFromEnd(automatic: Boolean = false) {
        val current = status.value
        if (closed || !current.ended || !privacy.canPlay) return
        if (automatic && (!canAutoReplay || inPip)) return
        canAutoReplay = false
        requestedPlaying = true
        pendingSeek = null
        mutableStatus.value =
            current.copy(
                stage = VideoStage.BUFFERING,
                ended = false,
                playing = false,
                timeline = current.timeline.copy(position = 0L),
            )
        engine.replay()
    }

    override fun toggleMuted() {
        if (closed || !sound.value.hasAudio) return
        val unmutingDuringPlayback = sound.value.muted && wantsPlayback && status.value.playing
        if (unmutingDuringPlayback && audio?.allowUnmute() == false) {
            pause()
            return
        }
        soundControl.toggleMuted()
        audio?.sync()
    }

    override fun seekTo(position: Long) {
        if (closed) return
        val target = position.coerceAtLeast(0L)
        if (status.value.seekable) {
            pendingSeek = null
            engine.seekTo(target.coerceAtMost(status.value.duration.coerceAtLeast(0L)))
        } else {
            pendingSeek = target
        }
    }

    fun pause() {
        if (closed) return
        canAutoReplay = false
        requestedPlaying = false
        audio?.sync()
        engine.pause()
    }

    override fun close() {
        if (closed) return
        closed = true
        opening.close()
        privacy.close()
        audio?.close()
        audio = null
        engine.close()
        if (ownsScope) scope.cancel()
    }
}

/** A session that outlives the screen that opened it (viewer → PiP → viewer) owns its own scope. */
internal fun ownedVideoSession(
    engine: VideoEngineBackend,
    url: String,
): VideoPlaybackSession = VideoPlaybackSession(engine, MainScope(), url, ownsScope = true)

/**
 * Сессия из запаса перешла на новую поверхность: превью держится до её первого кадра,
 * перемотка на ту же позицию перерисовывает кадр и на паузе.
 */
internal fun VideoPlaybackSession.reattach(playing: Boolean) {
    mutableVideoOutput.value = false
    val current = status.value
    // Вернулись к видео свайпом — оно начинается заново, позиция не запоминается.
    // Перемотка заодно рисует кадр на новой поверхности.
    if (current.seekable) seekTo(0L)
    if (playing && !current.playing) togglePlay()
    if (!playing) pause()
}

/** A paused session draws nothing on a fresh surface: seeking to the same spot redraws the frame. */
internal fun VideoPlaybackSession.redrawPaused() {
    val current = status.value
    if (!current.playing && current.seekable) seekTo(current.position)
}
