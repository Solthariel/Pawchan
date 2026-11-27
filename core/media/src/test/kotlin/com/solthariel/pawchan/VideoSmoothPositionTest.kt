package com.solthariel.pawchan

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.solthariel.pawchan.media.video.VIDEO_CONTROLS_TIMEOUT_MS
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoStage
import com.solthariel.pawchan.media.video.VideoTimeline
import com.solthariel.pawchan.media.video.followVideoPosition
import com.solthariel.pawchan.media.video.seekSettled
import com.solthariel.pawchan.media.video.smoothVideoPosition
import com.solthariel.pawchan.media.video.videoEdgeProgressRect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoSmoothPositionTest {
    @Test
    fun timeMovesBetweenPlayerPollsInsteadOfJumping() {
        assertEquals(1_100L, smoothVideoPosition(1_000, 100, 1f, 10_000))
        assertEquals(1_400L, smoothVideoPosition(1_000, 200, 2f, 10_000))
        assertEquals(1_050L, smoothVideoPosition(1_000, 100, 0.5f, 10_000))
    }

    @Test
    fun estimateNeverRunsFarAheadOrPastTheEnd() {
        assertEquals(1_400L, smoothVideoPosition(1_000, 5_000, 1f, 10_000))
        assertEquals(10_000L, smoothVideoPosition(9_900, 300, 1f, 10_000))
        assertEquals(1_000L, smoothVideoPosition(1_000, -50, 1f, 10_000))
    }

    @Test
    fun releasedThumbWaitsUntilThePlayerReachesTheChosenSpot() {
        assertFalse(seekSettled(position = 1_000, target = 5_000))
        assertTrue(seekSettled(position = 4_400, target = 5_000))
    }

    @Test
    fun controlsStayFiveSeconds() {
        assertEquals(5_000L, VIDEO_CONTROLS_TIMEOUT_MS)
    }

    @Test
    fun thinLineSitsOnTheBottomEdgeOfTheFrame() {
        val screen = IntSize(1080, 2400)
        val safe = IntRect(0, 0, 1080, 2400 - 126)
        assertEquals(IntRect(0, 1498, 1080, 1504), videoEdgeProgressRect(screen, 16.0 / 9.0, safe, 6))
        assertEquals(IntRect(60, 2268, 1020, 2274), videoEdgeProgressRect(screen, 0.4, safe, 6))
        assertEquals(IntRect(0, 2268, 1080, 2274), videoEdgeProgressRect(screen, null, safe, 6))
    }

    @Test
    fun smoothingStopsAskingForFramesSoonAfterThePoll() {
        val clock = StepFrameClock(stepMillis = 16L, limit = 1_000)
        val shown = mutableListOf<Long>()
        val status = VideoPlaybackStatus(VideoStage.READY, VideoTimeline(position = 1_000L), playing = true)
        runBlocking(clock) { followVideoPosition(status, 1f) { shown += it } }
        assertTrue(clock.frames < 40)
        assertEquals(1_400L, shown.last())
    }
}

private class StepFrameClock(
    private val stepMillis: Long,
    private val limit: Int,
) : MonotonicFrameClock {
    var frames = 0
        private set

    override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
        frames += 1
        check(frames <= limit) { "smoothing never stops asking for frames" }
        return onFrame(frames * stepMillis * NANOS_PER_MILLI)
    }
}

private const val NANOS_PER_MILLI = 1_000_000L
