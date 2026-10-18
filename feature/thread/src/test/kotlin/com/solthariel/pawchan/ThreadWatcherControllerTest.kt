package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadError
import com.solthariel.pawchan.thread.watch.ThreadWatcherController
import com.solthariel.pawchan.thread.watch.ThreadWatcherPoll
import com.solthariel.pawchan.thread.watch.ThreadWatcherStopReason
import com.solthariel.pawchan.thread.watch.ThreadWatcherUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadWatcherControllerTest {
    @Test
    fun pollsOnIntervalAndKeepsNewPostCountAcrossTemporaryError() =
        runTest {
            val results =
                ArrayDeque(
                    listOf(
                        ThreadWatcherPoll(newPosts = 2),
                        ThreadWatcherPoll(newPosts = 0, error = ThreadError.NETWORK),
                    ),
                )
            val controller =
                ThreadWatcherController(
                    scope = this,
                    intervalMillis = 1_000L,
                    poll = { results.removeFirst() },
                )

            controller.setEnabled(true)
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(0, controller.state.value.newPosts)

            controller.setActive(true)
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(2, controller.state.value.newPosts)

            controller.setActive(false)
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(null, controller.state.value.error)

            controller.setActive(true)
            advanceTimeBy(1_000L)
            runCurrent()
            assertEquals(2, controller.state.value.newPosts)
            assertEquals(ThreadError.NETWORK, controller.state.value.error)

            controller.setEnabled(false)
            controller.setActive(false)
            assertEquals(ThreadWatcherUiState(), controller.state.value)
        }

    @Test
    fun terminalResultStopsPolling() =
        runTest {
            var polls = 0
            val controller =
                ThreadWatcherController(
                    scope = this,
                    intervalMillis = 1_000L,
                    poll = {
                        polls += 1
                        ThreadWatcherPoll(
                            newPosts = 1,
                            stopReason = ThreadWatcherStopReason.CLOSED,
                        )
                    },
                )

            controller.setEnabled(true)
            controller.setActive(true)
            advanceTimeBy(2_000L)
            runCurrent()

            assertEquals(1, polls)
            assertEquals(false, controller.state.value.enabled)
            assertEquals(1, controller.state.value.newPosts)
            assertEquals(ThreadWatcherStopReason.CLOSED, controller.state.value.stopReason)
            controller.setActive(false)
        }
}
