package com.solthariel.pawchan.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockDelayChoicesTest {
    @Test fun defaultChoicesMatchTheAgreedOrderAndDurations() {
        val choices = appLockDelayChoices(AppLockDelay.MINUTE_1)
        assertEquals(
            listOf(0L, 60_000L, 300_000L, 900_000L, 3_600_000L, 10_800_000L, 21_600_000L),
            choices.map { it.millis },
        )
        assertEquals(choices.distinct(), choices)
    }

    @Test fun persistedLegacyDelaysRemainSelectedUntilTheUserChangesThem() {
        listOf(AppLockDelay.SECONDS_30, AppLockDelay.MINUTES_2, AppLockDelay.MINUTES_30).forEach { old ->
            assertEquals(old, AppLockDelay.fromMillis(old.millis))
            val options = appLockDelayChoices(old)
            assertTrue(old in options)
            assertEquals(options.sortedBy { it.millis }, options)
            assertEquals(8, options.size)
        }
    }

    @Test fun choosingANewDelayDoesNotKeepLegacyChoicesInTheMenu() {
        assertEquals(7, appLockDelayChoices(AppLockDelay.HOURS_6).size)
        assertEquals(AppLockDelay.HOURS_6, AppLockDelay.fromMillis(21_600_000L))
        assertEquals(AppLockDelay.MINUTE_1, AppLockDelay.fromMillis(Long.MAX_VALUE))
    }
}
