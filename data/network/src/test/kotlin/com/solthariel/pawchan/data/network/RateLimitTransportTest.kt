package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RateLimitTransportTest {
    @Test
    fun responseAndLocalGuardCarryTheSameRemainingPause() =
        runTest {
            val policy = ServerRequestBackoff({ 0L }, { 0L })
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(ServerBackoffInterceptor(policy))
                    .addInterceptor { limitedResponse(it.request(), "30") }
                    .build()
            val expected = PostingOutcome.Failure(PostingFailure.HttpStatus(429, 30_000L))
            assertEquals(expected, executePostingRequest(client, request()) { PostingOutcome.Success("ignored") })
            assertEquals(expected, executePostingRequest(client, request()) { PostingOutcome.Success("ignored") })
        }

    @Test
    fun missingRetryAfterUsesTheExistingPolicyFallback() =
        runTest {
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(ServerBackoffInterceptor(ServerRequestBackoff({ 0L })))
                    .addInterceptor { limitedResponse(it.request(), null) }
                    .build()
            assertEquals(
                PostingOutcome.Failure(PostingFailure.HttpStatus(429, 5_000L)),
                executePostingRequest(client, request()) { PostingOutcome.Success("ignored") },
            )
        }

    @Test
    fun rawResponseParsesRetryAfterButDoesNotInventAnUnknownDuration() {
        assertEquals(
            DomainOutcome.Failure(DomainFailure.HttpStatus(429, 30_000L)),
            limitedResponse(request(), "30").toDomainOutcome { "ignored" },
        )
        assertEquals(
            DomainOutcome.Failure(DomainFailure.HttpStatus(429)),
            limitedResponse(request(), null).toDomainOutcome { "ignored" },
        )
    }

    @Test
    fun unrelatedStatusesDoNotInheritCooldownMetadata() {
        val response = limitedResponse(request(), "30").newBuilder().code(503).build()
        assertEquals(
            PostingOutcome.Failure(PostingFailure.HttpStatus(503)),
            response.toPostingOutcome(30_000L) { PostingOutcome.Success("ignored") },
        )
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun cancellationDuringCooldownDoesNotOpenACall() =
        runTest {
            val policy = ServerRequestBackoff({ testScheduler.currentTime }, { 0L })
            policy.rateLimited("2ch.su", "4", null)
            val calls = AtomicInteger()
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor(ServerBackoffInterceptor(policy))
                    .addInterceptor {
                        calls.incrementAndGet()
                        limitedResponse(it.request(), "4")
                    }.build()
            val task = launch { executePostingRequest(client, request()) { PostingOutcome.Success("ignored") } }
            runCurrent()
            task.cancelAndJoin()
            assertTrue(task.isCancelled)
            assertEquals(0, calls.get())
        }

    private fun request(): Request = Request.Builder().url("https://2ch.su/user/posting").build()

    private fun limitedResponse(
        request: Request,
        retryAfter: String?,
    ): Response =
        Response
            .Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(429)
            .message("limited")
            .apply { retryAfter?.let { header("Retry-After", it) } }
            .body("".toResponseBody())
            .build()
}
