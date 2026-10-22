package com.solthariel.pawchan.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UpdateTransportTest {
    @get:Rule val temporary = TemporaryFolder()
    private val url = "https://example.org/update".toHttpUrl()

    @Test fun readsTextOffTheCallingThreadAndClosesTheBody() =
        runBlocking {
            val caller = Thread.currentThread()
            val body = TrackedUpdateBody("notes") { assertTrue(Thread.currentThread() !== caller) }
            withTransport(body) { transport -> assertEquals("notes", transport.readText(url)) }
            assertTrue(body.closed)
        }

    @Test fun rejectsKnownOversizedTextBeforeReading() =
        runBlocking {
            val body = TrackedUpdateBody("", MAX_UPDATE_TEXT_BYTES + 1)
            withTransport(body) { transport ->
                assertTrue(runCatching { transport.readText(url) }.exceptionOrNull() is IllegalStateException)
            }
            assertFalse(body.read)
            assertTrue(body.closed)
        }

    @Test fun unknownLengthTextStillHasABoundedRead() =
        runBlocking {
            val body = TrackedUpdateBody(" ".repeat(MAX_UPDATE_TEXT_BYTES.toInt() + 1), -1)
            withTransport(body) { transport ->
                assertTrue(runCatching { transport.readText(url) }.exceptionOrNull() is IllegalStateException)
            }
            assertTrue(body.closed)
        }

    @Test fun textExactlyAtTheLimitIsAccepted() =
        runBlocking {
            val text = "x".repeat(MAX_UPDATE_TEXT_BYTES.toInt())
            withTransport(text.toResponseBody()) { transport -> assertEquals(text, transport.readText(url)) }
        }

    @Test fun httpErrorClosesTheResponseWithoutReadingIt() =
        runBlocking {
            val body = TrackedUpdateBody("error")
            withTransport(body, 503) { transport ->
                assertTrue(runCatching { transport.readText(url) }.exceptionOrNull() is IllegalStateException)
            }
            assertFalse(body.read)
            assertTrue(body.closed)
        }

    @Test fun wrongContentLengthDoesNotCreateAnApk() =
        runBlocking {
            val target = temporary.root.resolve("partial.apk")
            withTransport("long".toResponseBody()) { transport ->
                assertTrue(runCatching { transport.downloadTo(url, target, 2) {} }.isFailure)
            }
            assertFalse(target.exists())
        }

    @Test fun unknownLengthApkCannotExceedTheDeclaredSize() =
        runBlocking {
            val target = temporary.root.resolve("partial.apk")
            withTransport(TrackedUpdateBody("too long", -1)) { transport ->
                assertTrue(runCatching { transport.downloadTo(url, target, 2) {} }.isFailure)
            }
            assertEquals(0L, target.length())
        }

    @Test fun cancellationBeforeHeadersCancelsTheActualHttpCall() =
        runBlocking {
            HeldUpdateServer(headers = false).use { server ->
                UpdateTransport(OkHttpClient()).use { transport ->
                    val request = launch { transport.readText(server.url) }
                    server.awaitRequest()
                    withTimeout(5000) { request.cancelAndJoin() }
                    server.awaitDisconnect()
                    assertTrue(request.isCancelled)
                }
            }
        }

    @Test fun cancellationDuringBodyReadClosesSocketAndCleansPartial() =
        runBlocking {
            HeldUpdateServer(headers = true).use { server ->
                val directory = temporary.newFolder()
                UpdateTransport(OkHttpClient()).use { transport ->
                    val store = UpdateDownloadStore(directory) { _, _ -> error("Must not verify") }
                    val release = UpdatePackage().apply { length = 6 }
                    val received = CountDownLatch(1)
                    val request =
                        launch {
                            store.download(release) { transport.downloadTo(server.url, it, 6) { received.countDown() } }
                        }
                    server.awaitRequest()
                    withContext(Dispatchers.IO) { assertTrue(received.await(5, TimeUnit.SECONDS)) }
                    withTimeout(5000) { request.cancelAndJoin() }
                    server.awaitDisconnect()
                    assertTrue(directory.listFiles().isNullOrEmpty())
                }
            }
        }

    @Test fun closeIsIdempotentAndRejectsNewRequests() =
        runBlocking {
            val client = OkHttpClient()
            val transport = UpdateTransport(client)
            transport.close()
            transport.close()
            assertTrue(runCatching { transport.readText(url) }.exceptionOrNull() is IllegalStateException)
            assertTrue(client.dispatcher.executorService.isShutdown)
            withContext(Dispatchers.IO) {
                assertTrue(client.dispatcher.executorService.awaitTermination(5, TimeUnit.SECONDS))
            }
        }

    private suspend fun withTransport(
        body: ResponseBody,
        code: Int = 200,
        test: suspend (UpdateTransport) -> Unit,
    ) {
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(code)
                            .message("Test")
                            .body(body)
                            .build()
                    },
                ).build()
        UpdateTransport(client).use { test(it) }
    }
}

private class TrackedUpdateBody(
    text: String,
    private val declaredLength: Long = text.toByteArray().size.toLong(),
    private val onRead: () -> Unit = {},
) : ResponseBody() {
    private val bytes = Buffer().writeUtf8(text)
    var closed = false
    var read = false

    override fun contentType(): okhttp3.MediaType? = null

    override fun contentLength(): Long = declaredLength

    override fun source(): BufferedSource {
        read = true
        onRead()
        return bytes
    }

    override fun close() {
        closed = true
        bytes.close()
    }
}
