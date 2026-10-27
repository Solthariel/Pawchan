package com.solthariel.pawchan.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockTimingTest {
    @Test fun foregroundBeforeOneMinuteDoesNotRelock() {
        var now = 0L
        val timing = AppLockTiming { now }
        timing.background()
        now = 59_999L
        assertFalse(timing.shouldLockOnForeground())
    }

    @Test fun oneMinuteRelocksAndConsumesTheBackgroundEvent() {
        var now = 100L
        val timing = AppLockTiming { now }
        timing.background()
        now += 60_000L
        assertTrue(timing.shouldLockOnForeground())
        assertFalse(timing.shouldLockOnForeground())
    }

    @Test fun foregroundWithoutBackgroundDoesNotRelock() {
        assertFalse(AppLockTiming { 123_000L }.shouldLockOnForeground())
    }

    @Test fun eachDelayRelocksExactlyAtItsBoundary() {
        AppLockDelay.entries.filter { it != AppLockDelay.IMMEDIATE }.forEach { delay ->
            var now = 100L
            val timing = AppLockTiming { now }
            timing.background()
            now += delay.millis - 1L
            assertFalse(delay.name, timing.shouldLockOnForeground(delay))
            timing.background()
            now += delay.millis
            assertTrue(delay.name, timing.shouldLockOnForeground(delay))
            assertFalse(delay.name, timing.shouldLockOnForeground(delay))
        }
    }

    @Test fun immediateRequiresBackgroundButNotAnElapsedInterval() {
        val timing = AppLockTiming { 100L }
        assertFalse(timing.shouldLockOnForeground(AppLockDelay.IMMEDIATE))
        timing.background()
        assertTrue(timing.shouldLockOnForeground(AppLockDelay.IMMEDIATE))
        assertFalse(timing.shouldLockOnForeground(AppLockDelay.IMMEDIATE))
    }

    @Test fun repeatedBackgroundDoesNotExtendTheUnlockedPeriod() {
        var now = 0L
        val timing = AppLockTiming { now }
        timing.background()
        now = 59_999L
        timing.background()
        now = 60_000L
        assertTrue(timing.shouldLockOnForeground())
    }

    @Test fun persistedDurationsAreExplicitAndUnknownValuesUseOneMinute() {
        val expected =
            listOf(0L, 30_000L, 60_000L, 120_000L, 300_000L, 1_800_000L, 3_600_000L, 900_000L, 10_800_000L, 21_600_000L)
        assertEquals(expected, AppLockDelay.entries.map { it.millis })
        AppLockDelay.entries.forEach { assertEquals(it, AppLockDelay.fromMillis(it.millis)) }
        listOf(-1L, 1L, Long.MAX_VALUE).forEach {
            assertEquals(AppLockDelay.MINUTE_1, AppLockDelay.fromMillis(it))
        }
    }
}
