package com.solthariel.pawchan.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAttemptPolicyTest {
    @Test fun firstFiveFailuresAreFreeAndEveryLaterFailureWaits() {
        var failures = 0
        repeat(5) {
            failures = PinAttemptPolicy.nextFailures(failures, false)
            assertFalse(PinAttemptPolicy.shouldDelay(failures))
        }
        repeat(100) {
            failures = PinAttemptPolicy.nextFailures(failures, false)
            assertTrue(PinAttemptPolicy.shouldDelay(failures))
        }
        assertEquals(6, failures)
    }

    @Test fun successResetsEvenASaturatedCounter() {
        assertEquals(0, PinAttemptPolicy.nextFailures(Int.MAX_VALUE, true))
        assertEquals(1, PinAttemptPolicy.nextFailures(-1, false))
        assertEquals(6, PinAttemptPolicy.nextFailures(Int.MAX_VALUE, false))
    }

    @Test fun deadlineExpiresAtTenSecondsAndImpossibleFutureIsIgnored() {
        assertEquals(10_000L, PinAttemptPolicy.remaining(100, 10_100))
        assertEquals(1L, PinAttemptPolicy.remaining(10_099, 10_100))
        assertEquals(0L, PinAttemptPolicy.remaining(10_100, 10_100))
        assertEquals(0L, PinAttemptPolicy.remaining(10_101, 10_100))
        assertEquals(0L, PinAttemptPolicy.remaining(0, 100_000))
    }
}
