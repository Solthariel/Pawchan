package com.solthariel.pawchan.media.video

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal enum class VideoEvent {
    OPENING,
    PLAYING,
    PAUSED,
    ENDED,
    BUFFERING,
    PROGRESS,
    VIDEO_OUTPUT,
    VIDEO_OUTPUT_PENDING,
    ERROR,
}

internal enum class VideoStage { BUFFERING, READY, ERROR }

internal data class VideoPlaybackStatus(
    val stage: VideoStage = VideoStage.BUFFERING,
    val position: Long = 0L,
    val duration: Long = 0L,
    val playing: Boolean = false,
    val ended: Boolean = false,
    val seekable: Boolean = false,
    val attempt: VideoAttempt = VideoAttempt(),
) {
    val software: Boolean get() = attempt.software
    val localSource: Boolean get() = attempt.local
}

/** Скорость воспроизведения: нажатие по кнопке перебирает 1x → 1.5x → 2x → 0.5x. */
internal enum class VideoSpeed(
    val label: String,
) {
    NORMAL("1x"),
    FAST("1.5x"),
    DOUBLE("2x"),
    SLOW("0.5x"),
    ;

    /** Скорость для нативного плеера: тысячные доли (1000 — обычная). */
    val milli: Int get() = (label.removeSuffix("x").toFloat() * MILLI_PER_SPEED).toInt()

    fun next(): VideoSpeed = entries[(ordinal + 1) % entries.size]
}

private const val MILLI_PER_SPEED = 1000f

/** Звук и скорость: живут отдельно от статуса движка, чтобы снимки движка их не затирали. */
internal data class VideoSoundState(
    val muted: Boolean = false,
    val speed: VideoSpeed = VideoSpeed.NORMAL,
)

/** Выключенный звук запоминается между видео до перезапуска процесса. */
internal object VideoSoundMemory {
    @Volatile var muted: Boolean = false
}

/** Настройка «Звук видео по умолчанию»: выставляет начальное состояние, дальше кнопка его запоминает. */
fun setVideoStartMuted(muted: Boolean) {
    VideoSoundMemory.muted = muted
}

internal interface VideoSoundControls {
    val sound: StateFlow<VideoSoundState>

    fun toggleMuted()

    fun cycleSpeed()

    fun setSpeed(speed: VideoSpeed)
}

/** Звук и скорость сессии; применяются к движку сразу и при каждом открытии файла. */
internal class VideoSound(
    private val engine: VideoEngineBackend,
) : VideoSoundControls {
    private val mutable = MutableStateFlow(VideoSoundState(muted = VideoSoundMemory.muted))
    override val sound: StateFlow<VideoSoundState> = mutable

    fun apply() {
        engine.speed(sound.value.speed.milli)
        engine.muted(sound.value.muted)
    }

    override fun toggleMuted() {
        val muted = !sound.value.muted
        VideoSoundMemory.muted = muted
        mutable.value = sound.value.copy(muted = muted)
        engine.muted(muted)
    }

    override fun cycleSpeed() = setSpeed(sound.value.speed.next())

    override fun setSpeed(speed: VideoSpeed) {
        mutable.value = sound.value.copy(speed = speed)
        engine.speed(speed.milli)
    }
}

internal interface VideoPlaybackControls : VideoSoundControls {
    val status: StateFlow<VideoPlaybackStatus>

    fun togglePlay()

    fun seekTo(position: Long)

    fun setSoftware(enabled: Boolean)

    fun retry()
}

internal data class VideoAttempt(
    val software: Boolean = false,
    val local: Boolean = true,
) {
    fun afterFailure(): VideoAttempt? =
        when {
            !software -> copy(software = true)
            else -> null
        }
}

internal interface VideoEngineBackend : AutoCloseable {
    fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    )

    fun attach(view: android.widget.FrameLayout)

    fun detach(view: android.widget.FrameLayout)

    fun play()

    fun pause()

    fun seekTo(position: Long)

    fun volume(percent: Int)

    fun muted(value: Boolean)

    fun speed(milli: Int)

    fun stop()

    /** Explicit, bounded main-thread snapshot of the rendered texture; no native decoder readback. */
    val posterFrame: android.graphics.Bitmap? get() = null

    /** Сведения о потоках открытого файла; пусто, пока файл не открыт. */
    val metadata: Map<String, String> get() = emptyMap()
}
