package com.solthariel.pawchan

import androidx.compose.runtime.MonotonicFrameClock
import com.solthariel.pawchan.thread.PreviewSwipeState
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PreviewSwipeAnimationTest {
    @Test fun smallFastPullRestoresWithoutCallingEitherCloseAction() =
        runTest {
            val state = state(1f)
            var closed = 0
            state.beginGesture()
            state.drag(20f)
            state.endGesture()
            withContext(PreviewTestFrameClock()) { state.release(9_000f, 1f, { closed++ }, { closed++ }) }
            assertEquals(0, closed)
            assertEquals(0f, state.offset, 0f)
        }

    @Test fun committedPullCallsOnlyItsDirectionActionAfterAnimationAtAnyDensity() =
        runTest {
            listOf(1f, 2.8125f, 4f).forEach { density ->
                listOf(-1f, 1f).forEach { direction ->
                    val state = state(density)
                    val calls = mutableListOf<String>()
                    state.beginGesture()
                    state.drag(direction * 400f * density)
                    state.endGesture()
                    withContext(PreviewTestFrameClock()) {
                        state.release(0f, density, { calls += "pop" }, { calls += "clear" })
                    }
                    assertEquals(listOf(if (direction < 0) "pop" else "clear"), calls)
                }
            }
        }

    @Test fun uncompletedPointerGestureNeverCommitsEvenWithEnoughDistance() =
        runTest {
            val state = state(1f)
            state.beginGesture()
            state.drag(500f)
            withContext(PreviewTestFrameClock()) { state.release(9_000f, 1f, { error("pop") }, { error("clear") }) }
            assertEquals(0f, state.offset, 0f)
        }

    @Test fun shortCardUsesTheViewportRatherThanAnEasyTinyCardThreshold() =
        runTest {
            val state = state(1f).apply { height = 50f }
            state.beginGesture()
            state.drag(100f)
            state.endGesture()
            withContext(PreviewTestFrameClock()) { state.release(0f, 1f, { error("pop") }, { error("clear") }) }
            assertEquals(0f, state.offset, 0f)
        }

    @Test fun cancelledAnimationNeverClosesAndReleasesItsSettlingGuard() =
        runTest {
            val state = state(1f)
            state.beginGesture()
            state.drag(500f)
            state.endGesture()
            var closed = 0
            val waiting =
                object : MonotonicFrameClock {
                    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = awaitCancellation()
                }
            val job = launch(waiting) { state.release(0f, 1f, { closed++ }, { closed++ }) }
            runCurrent()
            assertEquals(0f, state.drag(100f), 0f)
            job.cancelAndJoin()
            assertEquals(0, closed)
            assertEquals(0f, state.offset, 0f)
            assertEquals(100f, state.drag(100f), 0f)
            assertEquals(100f, state.offset, 0f)
        }

    @Test fun committedExitFinishesWithinItsDeadlineEvenIfFramesStop() =
        runTest {
            val state = state(1f)
            state.beginGesture()
            state.drag(500f)
            state.endGesture()
            var closed = 0
            val stalled =
                object : MonotonicFrameClock {
                    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = awaitCancellation()
                }
            val job = launch(stalled) { state.release(0f, 1f, { closed++ }, { closed++ }) }
            runCurrent()
            advanceTimeBy(149)
            runCurrent()
            assertEquals(0, closed)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(1, closed)
            job.join()
        }

    private fun state(density: Float) =
        PreviewSwipeState().apply {
            height = 500f * density
            viewportHeight = 700f * density
        }
}

private class PreviewTestFrameClock : MonotonicFrameClock {
    private var time = 0L

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        time += 16_000_000L
        return onFrame(time)
    }
}
