package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManualMirrorSummaryTest {
    @Test fun averagesOnlySuccessfulResponsesAndCountsMissingSamples() {
        val result =
            summarizeManualMirror(
                "host",
                listOf(
                    MirrorObservation("host", MirrorAvailability.AVAILABLE, 1, 100),
                    MirrorObservation("host", MirrorAvailability.NETWORK_ERROR, 2),
                    MirrorObservation("host", MirrorAvailability.AVAILABLE, 3, 300),
                ),
                3,
            )
        assertEquals(200L, requireNotNull(result.responseTimeMillis))
        assertEquals(2, requireNotNull(result.successfulSamples))
        assertEquals(3, requireNotNull(result.totalSamples))
    }

    @Test fun noSuccessDoesNotInventAnAverage() {
        val result =
            summarizeManualMirror(
                "host",
                listOf(
                    MirrorObservation("host", MirrorAvailability.SERVER_ERROR, 1, 99),
                ),
                3,
            )
        assertEquals(MirrorAvailability.SERVER_ERROR, result.availability)
        assertEquals(0, requireNotNull(result.successfulSamples))
        assertNull(result.responseTimeMillis)
    }
}
