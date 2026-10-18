package com.solthariel.pawchan.data.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServerRequestBackoffTest {
    @Test
    fun retryAfterSupportsSecondsAndHttpDatesWithServerClock() {
        assertEquals(12_000L, retryAfterMillis("12", null, 0L))
        assertEquals(1_000L, retryAfterMillis("0", null, 0L))
        assertNull(retryAfterMillis("-1", null, 0L))
        assertNull(retryAfterMillis("invalid", null, 0L))
        assertEquals(
            15_000L,
            retryAfterMillis("Sun, 04 Oct 2026 03:44:30 GMT", "Sun, 04 Oct 2026 03:44:15 GMT", 0L),
        )
    }

    @Test
    fun cooldownIsHostScopedAndExpiresMonotonically() {
        var now = 0L
        val policy = ServerRequestBackoff({ now }, { 0L })
        policy.rateLimited("2ch.su", "12", null)
        assertEquals(12_000L, policy.remainingMillis("2ch.su"))
        assertEquals(0L, policy.remainingMillis("example.org"))
        now = 5_000L
        assertEquals(7_000L, policy.remainingMillis("2ch.su"))
        now = 12_000L
        assertEquals(0L, policy.remainingMillis("2ch.su"))
    }

    @Test
    fun previewSlotsAreNotReservedWhileWaiting() {
        var now = 0L
        val policy = ServerRequestBackoff({ now }, { 0L })
        assertEquals(0L, policy.previewStartDelayMillis("2ch.su"))
        repeat(100) { assertEquals(250L, policy.previewStartDelayMillis("2ch.su")) }
        now = 250L
        assertEquals(0L, policy.previewStartDelayMillis("2ch.su"))
    }

    @Test
    fun fallbackEscalatesAndNeverShortensExistingServerPause() {
        var now = 0L
        val policy = ServerRequestBackoff({ now }, { 0L })
        policy.rateLimited("2ch.su", null, null)
        assertEquals(5_000L, policy.remainingMillis("2ch.su"))
        now = 5_000L
        policy.rateLimited("2ch.su", null, null)
        assertEquals(10_000L, policy.remainingMillis("2ch.su"))
        policy.rateLimited("2ch.su", "120", null)
        policy.rateLimited("2ch.su", "1", null)
        assertEquals(120_000L, policy.remainingMillis("2ch.su"))
    }

    @Test
    fun recoveryStartsSlowlyAndReturnsToNormalAfterQuietPeriod() {
        var now = 0L
        val policy = ServerRequestBackoff({ now }, { 0L })
        policy.rateLimited("2ch.su", "1", null)
        assertEquals(1_000L, policy.previewStartDelayMillis("2ch.su"))
        now = 1_000L
        assertEquals(0L, policy.previewStartDelayMillis("2ch.su"))
        assertEquals(1_000L, policy.previewStartDelayMillis("2ch.su"))
        now = 62_000L
        assertEquals(0L, policy.previewStartDelayMillis("2ch.su"))
        assertEquals(250L, policy.previewStartDelayMillis("2ch.su"))
    }
}
