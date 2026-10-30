package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import com.solthariel.pawchan.domain.network.MirrorObservation
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException
import java.io.InterruptedIOException

class MirrorProbeCallbackTest {
    private val request = Request.Builder().url("https://2ch.su/index.json").build()
    private val call = OkHttpClient().newCall(request)
    private val observations = mutableListOf<MirrorObservation>()

    @Test fun responseDurationUsesMonotonicTimeNotChangedWallClock() {
        var nanos = 1_000_000L
        var millis = 10_000L
        val callback = MirrorProbeCallback("2ch.su", observations::add, { nanos }, { millis })
        nanos += 123_999_999L
        millis = -50_000L
        callback.onResponse(call, response(200, """{"boards":[{"id":"b","name":"Бред"}]}"""))
        assertEquals(MirrorObservation("2ch.su", MirrorAvailability.AVAILABLE, -50_000, 123), observations.single())
    }

    @Test fun invalidOrRejectedResponseKeepsStatusAndMeasuredDuration() {
        val responses = listOf(200 to MirrorAvailability.INVALID_RESPONSE, 429 to MirrorAvailability.RATE_LIMITED)
        for ((code, expected) in responses) {
            var nanos = 0L
            val callback = MirrorProbeCallback("2ch.su", observations::add, { nanos }, { 100 })
            nanos = 45_000_000L
            callback.onResponse(call, response(code, "{}"))
            assertEquals(expected, observations.last().availability)
            assertEquals(45L, observations.last().responseTimeMillis)
        }
    }

    @Test fun timeoutAndNetworkFailureDoNotInventAnHttpResponseTime() {
        val callback = MirrorProbeCallback("2ch.su", observations::add)
        callback.onFailure(call, InterruptedIOException("timeout"))
        assertEquals(MirrorAvailability.TIMEOUT, observations.last().availability)
        assertNull(observations.last().responseTimeMillis)
        callback.onFailure(call, IOException("offline"))
        assertEquals(MirrorAvailability.NETWORK_ERROR, observations.last().availability)
        assertNull(observations.last().responseTimeMillis)
    }

    @Test fun cancelledTransportStillCompletesForTheRoundOwnerIncludingCallTimeout() {
        val callback = MirrorProbeCallback("2ch.su", observations::add)
        call.cancel()
        callback.onFailure(call, InterruptedIOException("timeout"))
        assertEquals(1, observations.size)
        assertEquals(MirrorAvailability.NETWORK_ERROR, observations.single().availability)
        assertNull(observations.single().responseTimeMillis)
    }

    private fun response(
        code: Int,
        body: String,
    ): Response =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body(body.toResponseBody())
            .build()
}
