package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Terminal interceptors supply responses in memory. These tests do not contact the server. */
class ServerBackoffInterceptorTest {
    @Test
    fun contentAndApiClonesShareOneCooldown() {
        val policy = ServerRequestBackoff({ 0L }, { 0L })
        val requests = AtomicInteger()
        val api = client(policy, requests)
        val content = DvachHttpClient.createContentClient(api)
        assertSame(api.serverBackoff(), content.serverBackoff())
        content.newCall(request()).execute().close()
        assertThrows(ServerCoolingDownException::class.java) { api.newCall(request()).execute() }
        assertEquals(1, requests.get())
    }

    @Test
    fun apiReports429DuringLongPauseWithoutOpeningAnotherCall() =
        runTest {
            val policy = ServerRequestBackoff({ 0L }, { 0L })
            policy.rateLimited("2ch.su", "120", null)
            val requests = AtomicInteger()
            val outcome = executeDomainRequest(client(policy, requests), request()) { "decoded" }
            assertEquals(DomainOutcome.Failure(DomainFailure.HttpStatus(429)), outcome)
            assertEquals(0, requests.get())
        }

    @Test
    fun postingIsNeverAutomaticallyReplayedAfter429() =
        runTest {
            val policy = ServerRequestBackoff({ 0L }, { 0L })
            val requests = AtomicInteger()
            val post = request().newBuilder().post("comment".toRequestBody()).build()
            val outcome = executePostingRequest(client(policy, requests), post) { PostingOutcome.Success("decoded") }
            assertEquals(PostingOutcome.Failure(PostingFailure.HttpStatus(429)), outcome)
            assertEquals(1, requests.get())
        }

    private fun request(): Request = Request.Builder().url("https://2ch.su/b/catalog.json").build()

    private fun client(
        policy: ServerRequestBackoff,
        requests: AtomicInteger,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(ServerBackoffInterceptor(policy))
            .addInterceptor { chain ->
                requests.incrementAndGet()
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("limited")
                    .header("Retry-After", "120")
                    .body("".toResponseBody())
                    .build()
            }.build()
}
