package com.solthariel.pawchan

import android.graphics.Bitmap
import android.widget.FrameLayout
import com.solthariel.pawchan.media.video.VideoEngineBackend
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackSession
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.ownedVideoSession

internal class PosterTestEngine(
    var frame: Bitmap? = null,
) : VideoEngineBackend {
    var snapshotReads = 0
        private set

    override val posterFrame: Bitmap?
        get() {
            snapshotReads += 1
            return frame
        }

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) = Unit

    override fun attach(view: FrameLayout) = Unit

    override fun detach(view: FrameLayout) = Unit

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(position: Long) = Unit

    override fun volume(percent: Int) = Unit

    override fun muted(value: Boolean) = Unit

    override fun speed(milli: Int) = milli

    override fun stop() = Unit

    override fun close() = Unit
}

internal fun posterTestSession(
    ready: Boolean,
    engine: PosterTestEngine = PosterTestEngine(),
): VideoPlaybackSession = ownedVideoSession(engine, "poster-test").apply { mutableVideoOutput.value = ready }
