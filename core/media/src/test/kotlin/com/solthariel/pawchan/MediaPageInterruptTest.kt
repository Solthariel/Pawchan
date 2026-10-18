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

/** Касание во время перелистывания не возвращает на прежний файл (быстрое листание видео → картинка). */
@OptIn(ExperimentalCoroutinesApi::class)
class MediaPageInterruptTest {
    @Test
    fun touchDuringCommittedPageCompletesNavigationInsteadOfBouncingBack() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(-150f, 0f))
            var next = 0
            motion.finish(ImageRelease.NEXT, Offset.Zero, MediaViewerNavigation(next = { next++ }), {})
            advanceTimeBy(40)
            runCurrent()
            assertEquals(0, next)
            assertTrue(motion.interrupt())
            assertEquals(1, next)
            advanceTimeBy(1_000)
            runCurrent()
            // Прерванная анимация не вызывает переход второй раз.
            assertEquals(1, next)
        }

    @Test
    fun touchDuringRestoreKeepsCurrentPage() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(-40f, 0f))
            var pages = 0
            val navigation = MediaViewerNavigation(next = { pages++ }, previous = { pages++ })
            motion.finish(ImageRelease.RESTORE, Offset.Zero, navigation, {})
            advanceTimeBy(40)
            runCurrent()
            assertFalse(motion.interrupt())
            assertEquals(0, pages)
        }

    @Test
    fun finishedPageLeavesNothingToComplete() =
        runTest {
            val motion = ImageViewerMotion(backgroundScope + Clock(testScheduler))
            motion.resize(ImageViewport(400f, 800f, 400f, 800f))
            motion.frame(shift = Offset(150f, 0f))
            var previous = 0
            motion.finish(ImageRelease.PREVIOUS, Offset.Zero, MediaViewerNavigation(previous = { previous++ }), {})
            advanceTimeBy(1_000)
            runCurrent()
            assertEquals(1, previous)
            assertFalse(motion.interrupt())
            assertEquals(1, previous)
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
