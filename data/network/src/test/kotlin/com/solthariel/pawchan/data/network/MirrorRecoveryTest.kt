package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import com.solthariel.pawchan.domain.settings.ForumConnectionPreferences
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MirrorRecoveryTest {
    @Test fun concurrentFailuresShareOneParallelRoundAndStayOnTheWinner() {
        val callbacks = linkedMapOf<String, (MirrorObservation) -> Unit>()
        val client = OkHttpClient()
        val recovery =
            MirrorRecovery("2ch.su", { host, report ->
                callbacks[host] = report
                client.newCall(Request.Builder().url("https://$host/index.json").build())
            }, {})
        val first = recovery.recover("2ch.su")
        val second = recovery.recover("2ch.su")
        assertSame(first, second)
        assertEquals(setOf("2ch.org", "2ch.life"), callbacks.keys)
        callbacks.getValue("2ch.life")(MirrorObservation("2ch.life", MirrorAvailability.AVAILABLE, 1))
        assertEquals("2ch.life", first.get())
        assertEquals("2ch.life", recovery.state.value.activeHost)
        callbacks.getValue("2ch.org")(MirrorObservation("2ch.org", MirrorAvailability.AVAILABLE, 2))
        assertEquals("2ch.life", recovery.state.value.activeHost)
        assertFalse(recovery.state.value.checking)
        assertEquals("2ch.life", recovery.recover("2ch.su").get())
        recovery.close()
    }

    @Test fun manualConfigurationInvalidatesAnOldRoundAndLateCallbacks() {
        var report: ((MirrorObservation) -> Unit)? = null
        val client = OkHttpClient()
        val recovery =
            MirrorRecovery("2ch.su", { host, callback ->
                report = callback
                client.newCall(Request.Builder().url("https://$host/index.json").build())
            }, {})
        val pending = recovery.recover("2ch.su")
        recovery.configure(ForumConnectionPreferences("2ch.org", false))
        assertNull(pending.get())
        report!!(MirrorObservation("2ch.life", MirrorAvailability.AVAILABLE, 1))
        assertEquals("2ch.org", recovery.state.value.activeHost)
        assertNull(recovery.recover("2ch.org").get())
        recovery.close()
    }

    @Test fun allUnavailableGivesOneFailureAndCooldownRatherThanALoop() {
        var probes = 0
        val client = OkHttpClient()
        val recovery =
            MirrorRecovery("2ch.su", { host, report ->
                probes++
                report(MirrorObservation(host, MirrorAvailability.NETWORK_ERROR, 1))
                client.newCall(Request.Builder().url("https://$host/index.json").build())
            }, {})
        assertNull(recovery.recover("2ch.su").get())
        assertNull(recovery.recover("2ch.su").get())
        assertEquals(2, probes)
        assertEquals("2ch.su", recovery.state.value.activeHost)
        recovery.close()
    }

    @Test fun manualCheckChecksAllWithoutSwitchingAndCloseCancelsOutstandingCalls() {
        val client = OkHttpClient()
        val calls = mutableListOf<okhttp3.Call>()
        val recovery =
            MirrorRecovery("custom.example", { host, _ ->
                client.newCall(Request.Builder().url("https://$host/index.json").build()).also { calls.add(it) }
            }, {})
        recovery.checkAvailability()
        recovery.checkAvailability()
        assertEquals(4, calls.size)
        assertEquals("custom.example", recovery.state.value.activeHost)
        recovery.close()
        assertTrue(calls.all { it.isCanceled() })
        assertFalse(recovery.state.value.checking)
    }
}
