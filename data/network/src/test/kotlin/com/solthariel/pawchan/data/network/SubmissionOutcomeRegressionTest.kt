package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.ReplyDraft
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.net.SocketTimeoutException

/** Uses only the pre-patch API, so failure on the base is behavioral, not a missing symbol. */
class SubmissionOutcomeRegressionTest {
    @Test
    fun timeoutAfterWritingAPostIsNotAnOrdinaryTimeout() =
        runBlocking {
            var attempts = 0
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        attempts++
                        requireNotNull(chain.request().body).writeTo(Buffer())
                        throw SocketTimeoutException("response lost after upload")
                    }.build()
            try {
                val result = DvachPostingApi.create(client).submitReply(address(), draft())
                assertNotEquals(PostingOutcome.Failure(PostingFailure.NetworkTimeout), result)
                assertEquals(1, attempts)
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }

    @Test
    fun emptySuccessBodyDoesNotMeanTheServerRejectedThePost() =
        runBlocking {
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("{}".toResponseBody())
                            .build()
                    }.build()
            try {
                val result = DvachPostingApi.create(client).submitReply(address(), draft())
                assertNotEquals(PostingOutcome.Failure(PostingFailure.Rejected(null)), result)
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }

    private fun address(): ThreadAddress =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )

    private fun draft() = ReplyDraft("keep draft", null, emptyList())
}
