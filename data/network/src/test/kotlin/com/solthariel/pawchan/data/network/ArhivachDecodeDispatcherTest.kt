package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

class ArhivachDecodeDispatcherTest {
    @Test fun payloadAndParserUseTheBackgroundDispatcher() =
        runBlocking {
            val dispatcher =
                Executors.newSingleThreadExecutor { Thread(it, "archive-decoder-test") }.asCoroutineDispatcher()
            try {
                val caller = Thread.currentThread()
                var payloadThread: Thread? = null
                var parseThread: Thread? = null
                val result =
                    decodeArchiveInBackground(
                        dispatcher,
                        payload = {
                            payloadThread = Thread.currentThread()
                            DomainOutcome.Success("данные")
                        },
                        parse = {
                            parseThread = Thread.currentThread()
                            it.length
                        },
                    )
                assertEquals(DomainOutcome.Success(6), result)
                assertTrue(parseThread?.name?.startsWith("archive-decoder-test") == true)
                assertEquals(payloadThread, parseThread)
                assertTrue(caller !== parseThread)
            } finally {
                dispatcher.close()
            }
        }

    @Test fun failuresPassThroughAndInvalidPayloadRemainsFailure() =
        runBlocking {
            var parsed = false
            val failure = DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
            val result =
                decodeArchiveInBackground(Dispatchers.Default, { failure }) {
                    parsed = true
                    it
                }
            assertEquals(failure, result)
            assertFalse(parsed)
            val invalid = decodeArchiveInBackground(Dispatchers.Default, { DomainOutcome.Success("invalid") }) { null }
            assertEquals(DomainOutcome.Failure(DomainFailure.InvalidResponse), invalid)
        }

    @Test fun cancelledParseNeverReturnsAStaleResult() =
        runBlocking {
            val dispatcher =
                Executors.newSingleThreadExecutor { Thread(it, "archive-cancel-test") }.asCoroutineDispatcher()
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            var delivered = false
            try {
                val task =
                    async(Dispatchers.Default) {
                        decodeArchiveInBackground(dispatcher, { DomainOutcome.Success("payload") }) {
                            started.countDown()
                            check(release.await(5, TimeUnit.SECONDS))
                            it
                        }
                        delivered = true
                    }
                assertTrue(started.await(5, TimeUnit.SECONDS))
                task.cancel()
                release.countDown()
                try {
                    task.await()
                    throw AssertionError("Cancelled parse returned normally")
                } catch (_: CancellationException) {
                    assertTrue(task.isCancelled)
                }
                assertFalse(delivered)
            } finally {
                release.countDown()
                dispatcher.close()
            }
        }

    @Test fun allArchiveReadingPathsUseTheDecodeDispatcher() =
        runBlocking {
            val dispatcher = CountingDispatcher()
            val client =
                OkHttpClient
                    .Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                        val body =
                            when (request.url.encodedPath) {
                                "/ajax/" -> {
                                    """pawchanTags({"ok":1,"tags":[]})"""
                                }

                                "/index/0/" -> {
                                    """<div class="thread_list"><table></table></div>"""
                                }

                                else -> {
                                    """<div class="post" id="post100">""" +
                                        """<div class="post_comment_body">Текст</div></div>"""
                                }
                            }
                        Response
                            .Builder()
                            .request(request)
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("ok")
                            .body(body.toResponseBody())
                            .build()
                    }.build()
            try {
                val browser = ArhivachBrowser(client, decodeDispatcher = dispatcher)
                val address =
                    ThreadAddress(
                        BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
                        requireNotNull(ThreadId.parse(100)),
                    )
                val external = ArhivachApi(client, dispatcher)
                val operations: List<suspend () -> DomainOutcome<*>> =
                    listOf(
                        { browser.searchTags("бред") },
                        { browser.loadThreads(emptyList(), 0) },
                        { browser.loadThread(55) },
                        { external.load(address, "https://arhivach.vc/thread/55/") },
                    )
                operations.forEach { operation ->
                    val before = dispatcher.count.get()
                    assertTrue(operation() is DomainOutcome.Success)
                    assertTrue(dispatcher.count.get() > before)
                }
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }
}

private class CountingDispatcher : CoroutineDispatcher() {
    val count = AtomicInteger()

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        count.incrementAndGet()
        Dispatchers.Default.dispatch(context, block)
    }
}
