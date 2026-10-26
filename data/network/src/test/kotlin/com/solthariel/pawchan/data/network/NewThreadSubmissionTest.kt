package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NewThreadSubmissionTest {
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b")))

    @Test
    fun threadSubmissionUsesTheSameUnconfirmedAndOneShotPolicy() =
        runBlocking {
            var attempts = 0
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        attempts++
                        val body = requireNotNull(chain.request().body)
                        assertTrue(body.isOneShot())
                        body.writeTo(Buffer())
                        throw IOException("response lost after thread upload")
                    }.build()
            try {
                assertEquals(
                    PostingOutcome.Failure(PostingFailure.SubmissionUnconfirmed),
                    DvachPostingApi.create(client).createThread(board, draft()),
                )
                assertEquals(1, attempts)
            } finally {
                close(client)
            }
        }

    @Test
    fun aConfirmedThreadIdentifierIsNotDowngradedToAWarning() =
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
                            .body("{\"thread\":123}".toResponseBody())
                            .build()
                    }.build()
            try {
                assertEquals(
                    PostingOutcome.Success(ThreadAddress(board, requireNotNull(ThreadId.parse(123)))),
                    DvachPostingApi.create(client).createThread(board, draft()),
                )
            } finally {
                close(client)
            }
        }

    private fun draft() =
        NewThreadDraft(
            "subject",
            "comment",
            null,
            listOf(ReplyAttachment("a.png", "image/png", byteArrayOf(1))),
        )

    private fun close(client: OkHttpClient) {
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
