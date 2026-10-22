package com.solthariel.pawchan.media.video

import android.content.Context
import android.widget.FrameLayout
import okhttp3.OkHttpClient
import java.io.File

/** Thin UI-thread control adapter; loading/native lifetime belong to its owner. */
internal class FfmpegVideoEngine(
    context: Context,
    client: OkHttpClient,
    directory: File,
) : VideoEngineBackend {
    private val state = FfmpegVideoState(context, client, directory)

    init {
        VideoHandoffDiagnostics.record(this, "engine_create state=${System.identityHashCode(state)}")
    }

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) = state.open(source, software, onEvent)

    override fun attach(view: FrameLayout) = state.output.attach(view, state.player, state.initialized)

    override fun detach(view: FrameLayout) = state.output.detach(view, state.player)

    override fun play() {
        state.requestedPlaying = true
        state.ended = false
        state.player?.controls?.setPlaying(true)
        state.emit(VideoEvent.PLAYING)
    }

    override fun pause() {
        state.requestedPlaying = false
        state.player?.controls?.setPlaying(false)
        state.emit(VideoEvent.PAUSED)
    }

    override fun seekTo(position: Long) {
        state.ended = false
        state.signalNextFrame()
        state.player?.controls?.setPosition(position.coerceAtLeast(0L))
    }

    override fun volume(percent: Int) {
        state.volume = percent.coerceIn(0, VIDEO_VOLUME_MAX_PERCENT)
        state.player?.controls?.setVolume(state.volume)
    }

    override fun muted(value: Boolean) {
        state.muted = value
        state.player?.controls?.setMuted(value)
    }

    override fun speed(milli: Int): Int {
        state.speed = state.player?.controls?.setPlaybackSpeed(milli) ?: milli
        return state.speed
    }

    override val posterFrame: android.graphics.Bitmap?
        get() = state.player?.video?.snapshot(VIDEO_POSTER_MAX_EDGE)

    override val metadata: Map<String, String>
        get() =
            state.player
                ?.facts
                ?.metadata
                .orEmpty()

    override fun stop() = state.stop()

    override fun close() = state.close()
}

internal const val VIDEO_VOLUME_MAX_PERCENT = 100
