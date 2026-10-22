package com.solthariel.pawchan.security

import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PinRetryCountdownTest {
    @Test fun countsDownWithoutEarlyUnlockOrExtraSecond() =
        runTest {
            val values = mutableListOf<Int>()
            val timer = PinRetryCountdown(backgroundScope, { testScheduler.currentTime }, values::add)
            timer.start(10_000)
            runCurrent()
            assertEquals(10, values.last())
            advanceTimeBy(9_999)
            assertEquals(1, values.last())
            advanceTimeBy(1)
            runCurrent()
            assertEquals(0, values.last())
        }

    @Test fun replacementCancelsOldTimerAndExplicitCancelClearsTheLabel() =
        runTest {
            val values = mutableListOf<Int>()
            val timer = PinRetryCountdown(backgroundScope, { testScheduler.currentTime }, values::add)
            timer.start(10_000)
            runCurrent()
            timer.start(1_001)
            runCurrent()
            assertEquals(2, values.last())
            advanceTimeBy(1_001)
            runCurrent()
            assertEquals(0, values.last())
            timer.start(10_000)
            runCurrent()
            timer.cancel()
            val afterCancel = values.toList()
            advanceTimeBy(20_000)
            runCurrent()
            assertEquals(afterCancel, values)
            assertEquals(0, values.last())
        }
}
