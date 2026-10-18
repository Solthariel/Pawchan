package com.solthariel.pawchan

import com.solthariel.pawchan.media.video.copyVideoBytes
import com.solthariel.pawchan.media.video.withLocalVideo
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class VideoLocalSourceTest {
    @Test
    fun fullGetHasCookiesNoRangeAndTemporaryFileIsDeletedOnClose() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            val request = CompletableDeferred<Pair<String?, String?>>()
            val payload = byteArrayOf(1, 2, 3, 4)
            server.createContext("/video") { exchange ->
                request.complete(
                    exchange.requestHeaders.getFirst("Cookie") to exchange.requestHeaders.getFirst("Range"),
                )
                exchange.sendResponseHeaders(200, payload.size.toLong())
                exchange.responseBody.use { it.write(payload) }
            }
            server.start()
            val directory = Files.createTempDirectory("video-source").toFile()
            val file = CompletableDeferred<File>()
            val client = OkHttpClient.Builder().cookieJar(TestVideoCookies()).build()
            try {
                val job =
                    launch {
                        withLocalVideo(client, directory, "http://127.0.0.1:${server.address.port}/video") {
                            file.complete(it)
                        }
                    }
                val ready = file.await()
                assertArrayEquals(payload, ready.readBytes())
                assertEquals("session=test", request.await().first)
                assertNull(request.await().second)
                job.cancelAndJoin()
                assertFalse(ready.exists())
                assertTrue(directory.listFiles().isNullOrEmpty())
            } finally {
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
                server.stop(0)
                directory.deleteRecursively()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun cancellationClosesBlockedHttpReadAndDeletesPartialFile() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            val started = CompletableDeferred<Unit>()
            val release = java.util.concurrent.CountDownLatch(1)
            server.createContext("/video") { exchange ->
                exchange.sendResponseHeaders(200, 1000L)
                exchange.responseBody.write(byteArrayOf(1))
                exchange.responseBody.flush()
                started.complete(Unit)
                release.await()
                exchange.close()
            }
            server.start()
            val directory = Files.createTempDirectory("video-cancel").toFile()
            val client = OkHttpClient()
            try {
                val job =
                    launch {
                        withLocalVideo(client, directory, "http://127.0.0.1:${server.address.port}/video") {
                            throw AssertionError("Partial video published")
                        }
                    }
                started.await()
                job.cancelAndJoin()
                assertTrue(directory.listFiles().isNullOrEmpty())
            } finally {
                release.countDown()
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
                server.stop(0)
                directory.deleteRecursively()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun truncatedVideoIsNotAccepted() =
        runTest {
            try {
                copyVideoBytes(ByteArrayInputStream(byteArrayOf(1)), ByteArrayOutputStream(), 20L)
                throw AssertionError("Truncated file accepted")
            } catch (_: IOException) {
                Unit
            }
        }

    @Test
    fun emptyVideoIsNotAccepted() =
        runTest {
            try {
                copyVideoBytes(ByteArrayInputStream(byteArrayOf()), ByteArrayOutputStream(), -1L)
                throw AssertionError("Empty file accepted")
            } catch (_: IOException) {
                Unit
            }
        }
}

private class TestVideoCookies : CookieJar {
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) = Unit

    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        listOf(
            Cookie
                .Builder()
                .name("session")
                .value("test")
                .hostOnlyDomain(url.host)
                .build(),
        )
}
