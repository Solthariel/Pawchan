package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ManualMirrorCheckTest {
    @Test fun samplesForThreeSecondsWithoutSwitchingAndCountsFailures() =
        runTest {
            val client = OkHttpClient()
            val counts = mutableMapOf<String, Int>()
            val recovery =
                MirrorRecovery("2ch.su", { host, report ->
                    val count = (counts[host] ?: 0) + 1
                    counts[host] = count
                    val availability = if (count == 2) MirrorAvailability.TIMEOUT else MirrorAvailability.AVAILABLE
                    val elapsed = if (count == 2) null else count * 100L
                    report(MirrorObservation(host, availability, count.toLong(), elapsed))
                    client.newCall(Request.Builder().url("https://$host/index.json").build())
                }, {}, StandardTestDispatcher(testScheduler))
            recovery.checkAvailability()
            runCurrent()
            assertTrue(recovery.state.value.manualChecking)
            assertEquals(1, requireNotNull(counts["2ch.su"]))
            advanceTimeBy(3_000)
            runCurrent()
            assertFalse(recovery.state.value.manualChecking)
            assertEquals(3, requireNotNull(counts["2ch.su"]))
            assertEquals("2ch.su", recovery.state.value.activeHost)
            val result =
                recovery.state.value.manualObservations
                    .first { it.host == "2ch.su" }
            assertEquals(2, requireNotNull(result.successfulSamples))
            assertEquals(3, requireNotNull(result.totalSamples))
            assertEquals(200L, requireNotNull(result.responseTimeMillis))
            recovery.close()
        }

    @Test fun automaticRecoveryIsNotBlockedByManualCheck() =
        runTest {
            val client = OkHttpClient()
            val callbacks = mutableListOf<Pair<String, (MirrorObservation) -> Unit>>()
            val recovery =
                MirrorRecovery("2ch.su", { host, report ->
                    callbacks.add(host to report)
                    client.newCall(Request.Builder().url("https://$host/index.json").build())
                }, {}, StandardTestDispatcher(testScheduler))
            recovery.checkAvailability()
            runCurrent()
            val auto = recovery.recover("2ch.su")
            assertEquals(5, callbacks.size)
            callbacks.last { it.first == "2ch.org" }.second(
                MirrorObservation("2ch.org", MirrorAvailability.AVAILABLE, 1, 200),
            )
            assertEquals("2ch.org", auto.get())
            assertEquals("2ch.org", recovery.state.value.activeHost)
            assertTrue(recovery.state.value.manualChecking)
            recovery.close()
            assertFalse(recovery.state.value.manualChecking)
        }
}
