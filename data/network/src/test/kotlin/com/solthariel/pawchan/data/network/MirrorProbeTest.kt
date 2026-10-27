package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.network.MirrorAvailability
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class MirrorProbeTest {
    @Test fun http200AloneDoesNotMeanAWorkingMirror() {
        assertEquals(MirrorAvailability.AVAILABLE, classify(200, """{"boards":[{"id":"b","name":"Бред"}]}"""))
        for (body in listOf("<html>challenge</html>", "{}", "{\"boards\":[]}", "[]")) {
            assertEquals(MirrorAvailability.INVALID_RESPONSE, classify(200, body))
        }
    }

    @Test fun verificationAndRateLimitAreNotReportedAsAvailable() {
        assertEquals(MirrorAvailability.VERIFICATION, classify(403, ""))
        assertEquals(MirrorAvailability.RATE_LIMITED, classify(429, ""))
        assertEquals(MirrorAvailability.SERVER_ERROR, classify(503, ""))
        assertEquals(MirrorAvailability.INVALID_RESPONSE, classify(302, ""))
    }

    @Test fun oversizedResponseIsRejected() {
        assertEquals(MirrorAvailability.INVALID_RESPONSE, classify(200, " ".repeat(1_048_577)))
    }

    private fun classify(
        code: Int,
        body: String,
    ): MirrorAvailability =
        Response
            .Builder()
            .request(Request.Builder().url("https://2ch.su/index.json").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body(body.toResponseBody())
            .build()
            .use(::classifyMirrorResponse)
}
