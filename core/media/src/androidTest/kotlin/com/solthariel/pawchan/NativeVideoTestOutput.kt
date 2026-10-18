package com.solthariel.pawchan

import android.graphics.Color
import com.solthariel.pawchan.media.video.FfmpegVideoEngine
import com.solthariel.pawchan.media.video.VideoEvent
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Observes the real engine, including a bounded snapshot on its very first VIDEO_OUTPUT callback. */
internal class NativeVideoTestOutput(
    private val name: String,
) {
    val failed = AtomicBoolean(false)
    val videoOutput = AtomicBoolean(false)
    val time = AtomicLong(0L)
    val duration = AtomicLong(0L)
    val rendered = AtomicInteger(0)
    val firstFrameValid = AtomicBoolean(false)

    fun record(
        engine: FfmpegVideoEngine,
        type: VideoEvent,
        status: VideoPlaybackStatus,
    ) {
        if (type == VideoEvent.ERROR) failed.set(true)
        if (type == VideoEvent.VIDEO_OUTPUT) {
            if (rendered.getAndIncrement() == 0) firstFrameValid.set(firstOutputMatchesFixture(engine))
            videoOutput.set(true)
        }
        time.set(status.position)
        duration.set(status.duration)
    }

    private fun firstOutputMatchesFixture(engine: FfmpegVideoEngine): Boolean {
        val poster = engine.posterFrame ?: return false
        try {
            val pixel = poster.getPixel(poster.width / 2, poster.height / 2)
            val brightness = Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)
            return if (name == "black_h264.mp4") brightness == 0 else brightness > 0
        } finally {
            poster.recycle()
        }
    }
}
