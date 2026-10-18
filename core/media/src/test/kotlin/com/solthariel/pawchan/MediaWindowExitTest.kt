package com.solthariel.pawchan

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import com.solthariel.pawchan.media.ImageRelease
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.ImageViewport
import com.solthariel.pawchan.media.MediaViewerNavigation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MediaWindowExitTest {
    @Test
    fun confirmedSwipeFadesWindowWithoutWaitingForFullPhotoTravel() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(0f, -110f), close = true)
            var closed = false
            motion.finish(ImageRelease.CLOSE, Offset.Zero, MediaViewerNavigation()) { closed = true }
            runCurrent()
            assertTrue(motion.dismissing)
            // The first synthetic frame starts the animation; inspect near its 150ms end.
            advanceTimeBy(140)
            runCurrent()
            assertFalse(closed)
            assertTrue(motion.windowAlpha < 0.1f)
            assertTrue(motion.displacement.y > -800f)
            advanceTimeBy(100)
            runCurrent()
            assertTrue(closed)
            assertEquals(0f, motion.windowAlpha, 0f)
            assertTrue(motion.displacement.y > -800f)
        }

    @Test
    fun uncommittedSwipeRestoresWithoutFadingWindow() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(0f, -30f), close = true)
            var closed = false
            motion.finish(ImageRelease.RESTORE, Offset.Zero, MediaViewerNavigation()) { closed = true }
            advanceTimeBy(250)
            runCurrent()
            assertFalse(closed)
            assertFalse(motion.dismissing)
            assertEquals(1f, motion.windowAlpha, 0f)
            assertEquals(Offset.Zero, motion.displacement)
        }

    @Test
    fun pinchLimitIsEightTimesNativeSizeForLargeAndSmallImages() {
        val large = ImageViewport(400f, 800f, 1600f, 3200f)
        val small = ImageViewport(400f, 800f, 100f, 200f)
        assertEquals(32f, large.maximumScale, 0f)
        assertEquals(2f, small.maximumScale, 0f)
        assertEquals(8f, large.maximumScale * large.fittedWidth / large.imageWidth, 0f)
        assertEquals(8f, small.maximumScale * small.fittedWidth / small.imageWidth, 0f)
    }

    @Test
    fun lateImageDimensionsAndRefreshCannotInterruptCommittedExit() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            val viewport = ImageViewport(400f, 800f, 400f, 800f)
            motion.resize(viewport)
            motion.frame(shift = Offset(0f, -110f), close = true)
            var closed = false
            motion.finish(ImageRelease.CLOSE, Offset.Zero, MediaViewerNavigation()) { closed = true }
            runCurrent()
            motion.resize(viewport.copy(imageHeight = 1600f))
            motion.reset()
            assertEquals(viewport, motion.viewport)
            assertTrue(motion.dismissing)
            advanceTimeBy(220)
            runCurrent()
            assertTrue(closed)
        }

    @Test
    fun downwardCloseReturnsThroughCloseAction() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(0f, 140f), close = true)
            var closed = 0
            motion.finish(ImageRelease.CLOSE, Offset.Zero, MediaViewerNavigation()) { closed++ }
            advanceTimeBy(250)
            runCurrent()
            assertEquals(1, closed)
        }

    @Test
    fun committedPageRetainsOutgoingOffsetUntilReplacement() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(-220f, 0f))
            var pages = 0
            motion.finish(ImageRelease.NEXT, Offset.Zero, MediaViewerNavigation(next = { pages++ })) {}
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(1, pages)
            assertEquals(-motion.pageExtent, motion.displacement.x, 0f)
            val next = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            next.resize(motion.viewport)
            assertEquals(Offset.Zero, next.displacement)
        }

    @Test
    fun lateDecodeDimensionsDoNotResetOrCancelPageAnimation() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(-220f, 0f))
            var pages = 0
            motion.finish(ImageRelease.NEXT, Offset.Zero, MediaViewerNavigation(next = { pages++ })) {}
            runCurrent()
            val before = motion.displacement
            motion.resize(motion.viewport.copy(imageWidth = 800f, imageHeight = 1600f))
            assertEquals(before, motion.displacement)
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(1, pages)
        }

    @Test
    fun cancelledPageRestoresCenterWithoutChangingSelection() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(-50f, 0f))
            var pages = 0
            motion.finish(ImageRelease.RESTORE, Offset.Zero, MediaViewerNavigation(next = { pages++ })) {}
            advanceTimeBy(1000)
            runCurrent()
            assertEquals(0, pages)
            assertEquals(Offset.Zero, motion.displacement)
        }

    private class Clock(
        private val scheduler: TestCoroutineScheduler,
    ) : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
            delay(16)
            return onFrame(scheduler.currentTime * 1_000_000)
        }
    }
}
