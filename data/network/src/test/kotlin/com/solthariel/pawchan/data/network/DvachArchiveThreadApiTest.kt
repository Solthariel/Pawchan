package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class DvachArchiveThreadApiTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )

    @Test
    fun missingLiveThreadFallsBackToReadOnlyArchiveWithDatedMedia() =
        runBlocking {
            val paths = CopyOnWriteArrayList<String>()
            val client = fixture(paths, 404)
            try {
                val result = DvachThreadApi.create(client).loadThread(address) as DomainOutcome.Success
                assertEquals(listOf("/b/res/42.json", "/b/arch/res/42.json"), paths)
                assertTrue(result.value.flags.archived)
                assertTrue(result.value.flags.closed)
                val file =
                    result.value.posts
                        .first()
                        .attachments
                        .single()
                assertEquals("https://2ch.su/b/arch/2026-10-01/src/42/image.png", file.remoteUrl.value)
                assertEquals("https://2ch.su/b/arch/2026-10-01/thumb/42/image.png", file.thumbnailUrl?.value)
            } finally {
                close(client)
            }
        }

    @Test
    fun serverFailureDoesNotTriggerArchiveRequest() =
        runBlocking {
            val paths = CopyOnWriteArrayList<String>()
            val client = fixture(paths, 503)
            try {
                assertTrue(DvachThreadApi.create(client).loadThread(address) is DomainOutcome.Failure)
                assertEquals(listOf("/b/res/42.json"), paths)
            } finally {
                close(client)
            }
        }

    @Test
    fun liveThreadDoesNotTriggerArchiveRequest() =
        runBlocking {
            val paths = CopyOnWriteArrayList<String>()
            val client = fixture(paths, 200)
            try {
                val result = DvachThreadApi.create(client).loadThread(address) as DomainOutcome.Success
                assertEquals(listOf("/b/res/42.json"), paths)
                assertEquals(false, result.value.flags.archived)
            } finally {
                close(client)
            }
        }

    private fun fixture(
        paths: MutableList<String>,
        liveStatus: Int,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                paths += request.url.encodedPath
                val archived = request.url.encodedPath.contains("/arch/")
                val responseRequest =
                    if (archived) {
                        request
                            .newBuilder()
                            .url("https://2ch.su/b/arch/2026-10-01/res/42.json")
                            .build()
                    } else {
                        request
                    }
                Response
                    .Builder()
                    .request(responseRequest)
                    .protocol(Protocol.HTTP_1_1)
                    .code(if (archived) 200 else liveStatus)
                    .message("fixture")
                    .body(PAYLOAD.toResponseBody())
                    .build()
            }.build()

    private fun close(client: OkHttpClient) {
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
    }
}

private const val PAYLOAD = """{"threads":[{"posts":[{"num":42,"parent":0,"subject":"Тест","comment":"Текст",
    "files":[{"md5":"a","name":"image.png","path":"/b/src/42/image.png",
    "thumbnail":"/b/thumb/42/image.png","size":1}]}]}]}"""
