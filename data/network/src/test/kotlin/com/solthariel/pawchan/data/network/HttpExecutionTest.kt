package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.serialization.SerializationException
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpExecutionTest {
    @Test
    fun domainResponseClassifiesNotFoundBeforeDecoding() {
        val outcome = response(404).toDomainOutcome { "decoded" }

        assertEquals(DomainOutcome.Failure(DomainFailure.NotFound), outcome)
    }

    @Test
    fun domainResponseClassifiesChallengeBeforeHttpStatus() {
        val outcome =
            response(
                code = 503,
                body = "<html><title>Just a moment...</title></html>",
                server = "cloudflare",
            ).toDomainOutcome { "decoded" }

        assertTrue((outcome as DomainOutcome.Failure).reason is DomainFailure.ProtectionChallenge)
    }

    @Test
    fun serializationFailureBecomesInvalidResponse() {
        val outcome =
            response(200, "{}").toDomainOutcome<String> {
                throw SerializationException("broken")
            }

        assertEquals(DomainOutcome.Failure(DomainFailure.InvalidResponse), outcome)
    }

    @Test
    fun postingResponseKeepsPostingSpecificHttpFailure() {
        val outcome =
            response(429).toPostingOutcome {
                PostingOutcome.Success("decoded")
            }

        assertEquals(
            PostingOutcome.Failure(PostingFailure.HttpStatus(429)),
            outcome,
        )
    }

    private fun response(
        code: Int,
        body: String = "",
        server: String? = null,
    ): Response {
        val builder =
            Response
                .Builder()
                .request(Request.Builder().url("https://2ch.su/b/catalog.json").build())
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("test")
                .body(body.toResponseBody())
        server?.let { builder.header("Server", it) }
        return builder.build()
    }
}
