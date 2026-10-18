package com.solthariel.pawchan.data.network

import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProtectionChallengeDetectorTest {
    @Test
    fun detectsCloudflareChallengeAndKeepsVerificationUrl() {
        val response =
            response(
                code = 403,
                contentType = "text/html",
                server = "cloudflare",
            )

        val challenge =
            requireNotNull(
                ProtectionChallengeDetector.detect(
                    response,
                    "<html><title>Just a moment...</title></html>",
                ),
            )

        assertEquals("cloudflare", challenge.provider)
        assertEquals("https://2ch.su/b/catalog.json", challenge.verificationUrl.value)
    }

    @Test
    fun doesNotMisclassifyOrdinaryServerFailure() {
        val response = response(code = 503, contentType = "application/json", server = "nginx")

        assertNull(ProtectionChallengeDetector.detect(response, "{\"error\":\"unavailable\"}"))
    }

    @Test
    fun ordinaryCloudflare429IsNotACaptcha() {
        val response = response(code = 429, contentType = "application/json", server = "cloudflare")
        assertNull(ProtectionChallengeDetector.detect(response, "{\"error\":\"rate limited\"}"))
    }

    @Test
    fun realChallengeDocumentOn429StillRequiresVerification() {
        val response = response(code = 429, contentType = "text/html", server = "cloudflare")
        val challenge = ProtectionChallengeDetector.detect(response, "<html>Just a moment...</html>")
        assertEquals("cloudflare", requireNotNull(challenge).provider)
    }

    private fun response(
        code: Int,
        contentType: String,
        server: String,
    ): Response =
        Response
            .Builder()
            .request(
                Request
                    .Builder()
                    .url("https://2ch.su/b/catalog.json")
                    .build(),
            ).protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .header("Content-Type", contentType)
            .header("Server", server)
            .body("".toResponseBody())
            .build()
}
