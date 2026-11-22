package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FourChanTransportTest {
    @Test fun rateLimitAndConditionalThreadRefresh() =
        runBlocking {
            var now = 0L
            val requests = mutableListOf<Request>()
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        requests += request
                        val status = if (requests.size == 3) 304 else 200
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(status)
                            .message("test")
                            .header("Last-Modified", "Wed, 01 Jan 2025 00:00:00 GMT")
                            .body("{\"posts\":[]}".toResponseBody())
                            .build()
                    }.build()
            val transport = FourChanTransport(client, { now }, { now += it })
            val path = listOf("g", "thread", "100.json")
            val first = transport.get(path, true)
            assertEquals(first, transport.get(path, true))
            assertEquals(1, requests.size)
            transport.get(listOf("boards.json"))
            assertEquals(1000L, now)
            now = 10000L
            assertEquals(first, transport.get(path, true))
            assertEquals(3, requests.size)
            assertEquals("Wed, 01 Jan 2025 00:00:00 GMT", requests.last().header("If-Modified-Since"))
            assertEquals("a.4cdn.org", requests.last().url.host)
            assertNull(requests.last().header("Cookie"))
            client.dispatcher.executorService.shutdown()
        }

    @Test fun invalidSnapshotDoesNotSupplyConditionalValidatorOnRetry() =
        runBlocking {
            var now = 0L
            val requests = mutableListOf<Request>()
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        requests += request
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .header("Last-Modified", "Wed, 01 Jan 2025 00:00:00 GMT")
                            .body((if (requests.size == 1) "invalid" else "valid").toResponseBody())
                            .build()
                    }.build()
            try {
                val transport = FourChanTransport(client, { now }, { now += it })
                val path = listOf("g", "thread", "100.json")
                val decode: (String) -> DomainOutcome<String> = { raw ->
                    if (raw == "valid") {
                        DomainOutcome.Success(raw)
                    } else {
                        DomainOutcome.Failure(DomainFailure.InvalidResponse)
                    }
                }
                assertEquals(DomainOutcome.Failure(DomainFailure.InvalidResponse), transport.fetch(path, true, decode))
                assertEquals(DomainOutcome.Success("valid"), transport.fetch(path, true, decode))
                assertEquals(10000L, now)
                assertNull(requests.last().header("If-Modified-Since"))
            } finally {
                client.dispatcher.executorService.shutdown()
            }
        }

    @Test fun unknownSitesNeverReachTheNetwork() =
        runBlocking {
            var calls = 0
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        calls++
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("test")
                            .body("{}".toResponseBody())
                            .build()
                    }.build()
            val api = FourChanReadApi(FourChanTransport(client))
            val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("g")))
            val thread = ThreadAddress(board, requireNotNull(ThreadId.parse(100)))
            assertEquals(DomainOutcome.Failure(DomainFailure.UnsupportedSite), api.loadCatalog(board))
            assertEquals(DomainOutcome.Failure(DomainFailure.UnsupportedSite), api.loadThread(thread))
            assertEquals(0, calls)
            client.dispatcher.executorService.shutdown()
        }

    @Test fun malformedAndMissingResponsesRemainFailures() =
        runBlocking {
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        val code =
                            if (chain
                                    .request()
                                    .url.encodedPath
                                    .contains("boards")
                            ) {
                                200
                            } else {
                                404
                            }
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(code)
                            .message("test")
                            .body("not json".toResponseBody())
                            .build()
                    }.build()
            var now = 0L
            val api = FourChanReadApi(FourChanTransport(client, { now }, { now += it }))
            assertEquals(DomainOutcome.Failure(DomainFailure.InvalidResponse), api.loadBoards())
            val board = BoardAddress(FOUR_CHAN_SITE, requireNotNull(BoardId.parse("g")))
            assertEquals(DomainOutcome.Failure(DomainFailure.NotFound), api.loadCatalog(board))
            assertTrue(now >= 1000)
            client.dispatcher.executorService.shutdown()
        }

    @Test fun cancellingAWaitDoesNotSendARequestOrBlockTheGate() =
        runBlocking {
            var now = 0L
            var calls = 0
            var block = true
            val waiting = CompletableDeferred<Unit>()
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        calls++
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("test")
                            .body("{}".toResponseBody())
                            .build()
                    }.build()
            val transport =
                FourChanTransport(client, { now }, { duration ->
                    if (block) {
                        waiting.complete(Unit)
                        awaitCancellation()
                    }
                    now += duration
                })
            transport.get(listOf("boards.json"))
            val cancelled = launch { transport.get(listOf("g", "catalog.json")) }
            waiting.await()
            cancelled.cancelAndJoin()
            assertEquals(1, calls)
            block = false
            transport.get(listOf("g", "catalog.json"))
            assertEquals(2, calls)
            client.dispatcher.executorService.shutdown()
        }
}
