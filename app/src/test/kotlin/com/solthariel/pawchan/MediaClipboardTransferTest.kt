package com.solthariel.pawchan

import com.solthariel.pawchan.infrastructure.media.copyClipboardBytes
import com.solthariel.pawchan.infrastructure.media.writeClipboardImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MediaClipboardTransferTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun writesPayloadAtExactLimit() =
        runTest {
            val payload = "image".encodeToByteArray()
            val output = ByteArrayOutputStream()
            copyClipboardBytes(payload.inputStream(), output, payload.size.toLong())
            assertArrayEquals(payload, output.toByteArray())
        }

    @Test
    fun rejectsUnknownLengthOverflowBeforeWritingChunk() =
        runTest {
            val output = ByteArrayOutputStream()
            try {
                copyClipboardBytes("too-large".byteInputStream(), output, 3L)
                error("Expected byte limit rejection")
            } catch (_: IOException) {
                assertEquals(0, output.size())
            }
        }

    @Test
    fun cancellationIsNotReportedAsSuccess() =
        runTest {
            var cancelled = false
            val job =
                launch {
                    cancel()
                    try {
                        copyClipboardBytes("image".byteInputStream(), ByteArrayOutputStream())
                    } catch (_: CancellationException) {
                        cancelled = true
                    }
                }
            job.join()
            assertTrue(cancelled)
        }

    @Test
    fun successfulResponseReplacesTargetWithoutLeavingStagingFiles() =
        runTest {
            val target = folder.newFile("image.png").apply { writeText("old-image") }
            writeClipboardImage(clientReturning(200, "new-image"), request(), target)
            assertEquals("new-image", target.readText())
            assertEquals(listOf("image.png"), folder.root.list()?.toList())
        }

    @Test
    fun httpFailurePreservesPreviousClipboardFile() =
        runTest {
            val target = folder.newFile("image.png").apply { writeText("old-image") }
            try {
                writeClipboardImage(clientReturning(404, "not-found"), request(), target)
                error("Expected HTTP failure")
            } catch (_: IOException) {
                assertEquals("old-image", target.readText())
                assertEquals(listOf("image.png"), folder.root.list()?.toList())
            }
        }

    @Test
    fun cancellingTransferCancelsHttpCallBeforeResponseArrives() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            val release = CountDownLatch(1)
            val client =
                OkHttpClient
                    .Builder()
                    .eventListener(
                        object : EventListener() {
                            override fun canceled(call: Call) {
                                cancelled.complete(Unit)
                            }
                        },
                    ).addInterceptor { chain ->
                        started.complete(Unit)
                        check(release.await(10, TimeUnit.SECONDS))
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("fixture")
                            .body("image".toResponseBody())
                            .build()
                    }.build()
            val target = folder.newFile("image.png").apply { writeText("old-image") }
            val job = launch(Dispatchers.IO) { writeClipboardImage(client, request(), target) }
            try {
                withTimeout(10_000L) { started.await() }
                job.cancel()
                withTimeout(10_000L) { cancelled.await() }
            } finally {
                job.cancel()
                release.countDown()
                job.join()
            }
            assertTrue(job.isCancelled)
            assertEquals("old-image", target.readText())
            assertEquals(listOf("image.png"), folder.root.list()?.toList())
        }

    private fun request(): Request = Request.Builder().url("https://example.invalid/image").build()

    private fun clientReturning(
        code: Int,
        payload: String,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("fixture")
                    .body(payload.toResponseBody())
                    .build()
            }.build()
}
