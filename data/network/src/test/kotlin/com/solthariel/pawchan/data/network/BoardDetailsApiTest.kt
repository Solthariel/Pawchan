package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class BoardDetailsApiTest {
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("biz")))

    @Test
    fun usesConfirmedBoardAndArchiveEndpoints() =
        runBlocking {
            val paths = CopyOnWriteArrayList<String>()
            val client = fixture(paths, 200)
            try {
                val api = DvachBoardDetailsApi.create(client)
                assertTrue(api.loadInformation(board) is DomainOutcome.Success)
                assertTrue(api.loadArchive(board) is DomainOutcome.Success)
                assertTrue(api.loadArchive(board, 2) is DomainOutcome.Success)
                assertEquals(listOf("/biz/index.json", "/biz/arch/index.json", "/biz/arch/2.json"), paths)
            } finally {
                close(client)
            }
        }

    @Test
    fun rateLimitAndMissingArchiveRemainTypedFailures() =
        runBlocking {
            listOf(429, 404).forEach { status ->
                val client = fixture(CopyOnWriteArrayList(), status)
                try {
                    val result = DvachBoardDetailsApi.create(client).loadArchive(board)
                    val expected = if (status == 404) DomainFailure.NotFound else DomainFailure.HttpStatus(status)
                    assertEquals(DomainOutcome.Failure(expected), result)
                } finally {
                    close(client)
                }
            }
        }

    private fun fixture(
        paths: MutableList<String>,
        status: Int,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val path = chain.request().url.encodedPath
                paths += path
                val body =
                    if (path == "/biz/index.json") {
                        """{"board":{"id":"biz","name":"Бизнес"}}"""
                    } else {
                        """{"threads":[],"pages":[0,1,2]}"""
                    }
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(status)
                    .message("fixture")
                    .body(body.toResponseBody())
                    .build()
            }.build()

    private fun close(client: OkHttpClient) {
        client.dispatcher.executorService.shutdownNow()
        client.connectionPool.evictAll()
    }
}
