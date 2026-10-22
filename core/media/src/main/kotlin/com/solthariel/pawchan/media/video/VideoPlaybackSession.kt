package com.solthariel.pawchan.media.video

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

internal class VideoPlaybackSession(
    val engine: VideoEngineBackend,
    private val scope: CoroutineScope,
    val url: String,
    private val soundControl: VideoSound = VideoSound(engine),
    private val ownsScope: Boolean = false,
) : VideoPlaybackControls,
    VideoSoundControls by soundControl,
    AutoCloseable {
    internal var audio: VideoPlaybackAudio? = null
    val privacy = VideoSessionPrivacy(scope, ::pause)
    private val mutableStatus = MutableStateFlow(VideoPlaybackStatus())
    override val status: StateFlow<VideoPlaybackStatus> = mutableStatus

    /** Первый кадр на текущей поверхности уже показан; до него на месте видео лежит превью. */
    val mutableVideoOutput = MutableStateFlow(false)
    val hasVideoOutput: StateFlow<Boolean> = mutableVideoOutput
    private var attempt = VideoAttempt()
    private var opening: Job? = null
    private var pendingSeek: Long? = null
    private var requestedPlaying = true
    private var closed = false

    /** The PiP window holds this session: the viewer must not pause, close or re-attach it. */
    @Volatile var inPip = false

    /** Запуск с заданной позиции — например, после возврата из «картинки в картинке». */
    fun startAt(
        position: Long,
        playing: Boolean = true,
    ) {
        pendingSeek = position.takeIf { it > 0L }
        requestedPlaying = playing
        start()
    }

    fun start() {
        if (closed) return
        soundControl.hasAudio = false
        engine.stop()
        opening?.cancel()
        mutableVideoOutput.value = false
        mutableStatus.value =
            status.value.copy(
                stage = VideoStage.BUFFERING,
                playing = false,
                seekable = false,
                attempt = attempt,
                ended = false,
            )
        opening =
            scope.launch {
                try {
                    open(url)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: IOException) {
                    mutableStatus.value = status.value.copy(stage = VideoStage.ERROR, playing = false)
                } catch (_: IllegalArgumentException) {
                    mutableStatus.value = status.value.copy(stage = VideoStage.ERROR, playing = false)
                }
            }
    }

    private fun open(source: String) {
        soundControl.apply()
        engine.open(source, attempt.software, ::onEvent)
        if (!requestedPlaying) engine.pause()
    }

    private fun onEvent(
        type: VideoEvent,
        snapshot: VideoPlaybackStatus,
    ) {
        if (closed) return
        soundControl.hasAudio = engine.metadata.containsKey("audio_format")
        if (type == VideoEvent.INCOMPLETE) requestedPlaying = false
        if (type == VideoEvent.ERROR) {
            recover()
        } else {
            if (type == VideoEvent.OPENING) mutableVideoOutput.value = false
            if (type == VideoEvent.VIDEO_OUTPUT) mutableVideoOutput.value = true
            // Новое окно видео пустое до первого кадра: до тех пор виден постер, а не чёрный экран.
            if (type == VideoEvent.VIDEO_OUTPUT_PENDING) mutableVideoOutput.value = false
            val stage = if (type == VideoEvent.OPENING) VideoStage.BUFFERING else snapshot.stage
            mutableStatus.value =
                snapshot.copy(
                    stage = stage,
                    attempt = attempt,
                    ended = type == VideoEvent.ENDED || snapshot.ended,
                )
            if (snapshot.seekable) {
                pendingSeek?.let {
                    pendingSeek = null
                    engine.seekTo(it)
                }
            }
            if (type == VideoEvent.PLAYING && !requestedPlaying) engine.pause()
        }
    }

    private fun recover() {
        val next = attempt.afterFailure()
        if (next == null) {
            engine.stop()
            mutableStatus.value = status.value.copy(stage = VideoStage.ERROR, playing = false)
        } else {
            pendingSeek = pendingSeek ?: status.value.position
            attempt = next
            start()
        }
    }

    override fun retry() {
        pendingSeek = status.value.position
        start()
    }

    override fun setSoftware(enabled: Boolean) {
        if (enabled != attempt.software) {
            pendingSeek = status.value.position
            attempt = attempt.copy(software = enabled)
            start()
        }
    }

    override fun togglePlay() {
        requestedPlaying = !status.value.playing
        if (requestedPlaying) {
            if (status.value.stage == VideoStage.ERROR) {
                retry()
            } else if (status.value.ended) {
                mutableStatus.value = status.value.copy(ended = false)
                engine.seekTo(0L)
                start()
            } else {
                engine.play()
            }
        } else {
            engine.pause()
        }
    }

    override fun seekTo(position: Long) {
        if (status.value.seekable) engine.seekTo(position.coerceIn(0L, status.value.duration))
    }

    fun pause() {
        requestedPlaying = false
        audio?.update(false)
        engine.pause()
    }

    override fun close() {
        closed = true
        privacy.close()
        audio?.close()
        audio = null
        opening?.cancel()
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
