package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class SubmissionAttemptTest {
    @Test
    fun uploadFailureIsUnconfirmedAndTheBodyIsNotReplayable() =
        runBlocking {
            withClient({ request ->
                val body = requireNotNull(request.body)
                assertTrue(body.isOneShot())
                val bytes = Buffer()
                body.writeTo(bytes)
                assertEquals("payload", bytes.readUtf8())
                assertEquals(7L, body.contentLength())
                throw SocketTimeoutException("response lost")
            }) { client ->
                assertEquals(unconfirmed(), executeSubmissionRequest(client, request(), ::decode))
            }
        }

    @Test
    fun failureBeforeWritingIsStillAnOrdinaryNetworkError() =
        runBlocking {
            withClient({ throw IOException("connection refused before upload") }) { client ->
                assertEquals(
                    PostingOutcome.Failure(PostingFailure.NetworkUnavailable),
                    executeSubmissionRequest(client, request(), ::decode),
                )
            }
        }

    @Test
    fun malformedOrUnidentifiableSuccessIsUnconfirmed() =
        runBlocking {
            for (payload in listOf("", "{broken", "{}", "[]")) {
                withClient({ response(it, 200, payload) }) { client ->
                    assertEquals(unconfirmed(), executeSubmissionRequest(client, request(), ::decode))
                }
            }
        }

    @Test
    fun aReadFailureAfterUploadIsUnconfirmed() =
        runBlocking {
            withClient({ request ->
                requireNotNull(request.body).writeTo(Buffer())
                response(request, 200, "").newBuilder().body(UnreadableBody()).build()
            }) { client ->
                assertEquals(unconfirmed(), executeSubmissionRequest(client, request(), ::decode))
            }
        }

    @Test
    fun explicitRejectionAndRateLimitStayDefiniteFailures() =
        runBlocking {
            withClient({ response(it, 200, "{\"error\":{\"code\":5}}") }) { client ->
                assertEquals(
                    PostingOutcome.Failure(PostingFailure.CaptchaRejected),
                    executeSubmissionRequest(client, request(), ::decode),
                )
            }
            withClient({ response(it, 429, "") }) { client ->
                assertEquals(
                    PostingOutcome.Failure(PostingFailure.HttpStatus(429)),
                    executeSubmissionRequest(client, request(), ::decode),
                )
            }
        }

    @Test
    fun serverFailureDoesNotProveTheSubmissionWasRejected() =
        runBlocking {
            withClient({ response(it, 502, "gateway lost response") }) { client ->
                assertEquals(unconfirmed(), executeSubmissionRequest(client, request(), ::decode))
            }
        }

    @Test
    fun confirmedPostKeepsItsIdentifierAndCaptchaKeepsItsOrdinaryTimeout() =
        runBlocking {
            withClient({ response(it, 200, "{\"num\":123}") }) { client ->
                val result = executeSubmissionRequest(client, request(), ::decode)
                assertTrue(result is PostingOutcome.Success)
            }
            withClient({ throw SocketTimeoutException("captcha endpoint") }) { client ->
                assertEquals(
                    PostingOutcome.Failure(PostingFailure.NetworkTimeout),
                    executePostingRequest(client, request(), ::decode),
                )
            }
        }

    @Test
    fun cancellationIsNotConvertedIntoAnUnconfirmedFailure() =
        runBlocking {
            val enqueued = CompletableDeferred<Unit>()
            val cancelled = AtomicBoolean()
            val factory = Call.Factory { request -> PendingSubmissionCall(request, enqueued, cancelled) }
            val job =
                launch {
                    executeSubmissionRequest(factory, request(), ::decode)
                    error("Cancellation must not produce a normal outcome")
                }
            enqueued.await()
            job.cancelAndJoin()
            assertTrue(job.isCancelled)
            assertTrue(cancelled.get())
        }
}

private suspend fun withClient(
    reply: (Request) -> Response,
    check: suspend (OkHttpClient) -> Unit,
) {
    val client = OkHttpClient.Builder().addInterceptor { chain -> reply(chain.request()) }.build()
    try {
        check(client)
    } finally {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}

private fun request(): Request =
    Request
        .Builder()
        .url("https://2ch.su/makaba/posting.fcgi")
        .post("payload".toRequestBody())
        .build()

private fun decode(payload: String) = PostingPayloadDecoder(kotlinx.serialization.json.Json).decodeReply(payload)

private fun unconfirmed() = PostingOutcome.Failure(PostingFailure.SubmissionUnconfirmed)

private fun response(
    request: Request,
    code: Int,
    payload: String,
): Response =
    Response
        .Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message("test")
        .body(payload.toResponseBody())
        .build()

private class UnreadableBody : ResponseBody() {
    override fun contentType(): MediaType? = null

    override fun contentLength() = -1L

    override fun source(): BufferedSource =
        object : ForwardingSource(Buffer()) {
            override fun read(
                sink: Buffer,
                byteCount: Long,
            ): Long = throw IOException("truncated response")
        }.buffer()
}

private class PendingSubmissionCall(
    private val request: Request,
    private val enqueued: CompletableDeferred<Unit>,
    private val cancelled: AtomicBoolean,
) : Call {
    override fun request() = request

    override fun enqueue(responseCallback: Callback) {
        requireNotNull(request.body).writeTo(Buffer())
        enqueued.complete(Unit)
    }

    override fun cancel() {
        cancelled.set(true)
    }

    override fun isCanceled() = cancelled.get()

    override fun isExecuted() = enqueued.isCompleted

    override fun timeout() = Timeout.NONE

    override fun clone(): Call = error("No retry/clone is expected")

    override fun execute(): Response = error("Only async execution is expected")
}
