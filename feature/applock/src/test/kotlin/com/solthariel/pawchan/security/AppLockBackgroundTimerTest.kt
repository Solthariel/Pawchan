package com.solthariel.pawchan.security

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockBackgroundTimerTest {
    @Test
    fun immediatePolicyRevokesSynchronously() =
        runTest {
            var locks = 0
            val timer = AppLockBackgroundTimer(backgroundScope) { locks += 1 }
            timer.start(0)
            assertEquals(1, locks)
        }

    @Test
    fun deadlineRevokesWhileHostRemainsInBackground() =
        runTest {
            var locks = 0
            val timer = AppLockBackgroundTimer(backgroundScope) { locks += 1 }
            timer.start(30_000)
            runCurrent()
            advanceTimeBy(29_999)
            runCurrent()
            assertEquals(0, locks)
            advanceTimeBy(1)
            runCurrent()
            assertEquals(1, locks)
        }

    @Test
    fun foregroundCancelsPendingDeadline() =
        runTest {
            var locks = 0
            val timer = AppLockBackgroundTimer(backgroundScope) { locks += 1 }
            timer.start(30_000)
            runCurrent()
            advanceTimeBy(1_000)
            timer.cancel()
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(0, locks)
        }

    @Test
    fun ownerDestructionRevokesOnceAndCancelsTheTimer() =
        runTest {
            var locks = 0
            val timer = AppLockBackgroundTimer(backgroundScope) { locks += 1 }
            timer.start(30_000)
            runCurrent()
            timer.lock()
            advanceTimeBy(60_000)
            runCurrent()
            assertEquals(1, locks)
        }
}
