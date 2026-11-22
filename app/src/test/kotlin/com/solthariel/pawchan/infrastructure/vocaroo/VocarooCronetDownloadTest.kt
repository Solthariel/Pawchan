package com.solthariel.pawchan.infrastructure.vocaroo

import com.solthariel.pawchan.thread.vocaroo.VocarooHttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class VocarooCronetDownloadTest {
    private class FakeCronet {
        val request: UrlRequest = mock(UrlRequest::class.java)
        val info: UrlResponseInfo = mock(UrlResponseInfo::class.java)
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        val reading = CountDownLatch(1)
        val buffer = AtomicReference<ByteBuffer>()
        lateinit var callback: UrlRequest.Callback
        lateinit var executor: Executor

        init {
            `when`(info.httpStatusCode).thenReturn(200)
            `when`(info.allHeaders).thenReturn(emptyMap())
            doAnswer {
                started.countDown()
                null
            }.`when`(request).start()
            doAnswer {
                cancelled.countDown()
                callback.onCanceled(request, info)
                null
            }.`when`(request).cancel()
            doAnswer {
                buffer.set(it.getArgument(0))
                reading.countDown()
                null
            }.`when`(request).read(any(ByteBuffer::class.java))
        }

        fun create(
            callback: UrlRequest.Callback,
            executor: Executor,
        ): UrlRequest {
            this.callback = callback
            this.executor = executor
            return request
        }

        fun response() = executor.execute { callback.onResponseStarted(request, info) }

        fun deliver(value: String) =
            executor.execute {
                val b = buffer.get()
                b.put(value.toByteArray())
                callback.onReadCompleted(request, info, b)
                callback.onSucceeded(request, info)
            }
    }

    private fun file(): File = Files.createTempDirectory("cronet").resolve("record.part").toFile()

    private fun CountDownLatch.awaitEvent() = assertTrue("Timed out waiting for event", await(3, TimeUnit.SECONDS))

    @Test fun cancellationWhileWaitingForHeadersCancelsRequest() =
        runBlocking {
            val fake = FakeCronet()
            val target = file()
            val job = launch(Dispatchers.IO) { downloadWithCronet(target, {}, fake::create) }
            fake.started.awaitEvent()
            job.cancelAndJoin()
            fake.cancelled.awaitEvent()
            assertFalse(target.exists())
        }

    @Test
    fun cancellationDuringRequestCreationDoesNotLeaveItRunning() =
        runBlocking {
            val fake = FakeCronet()
            val creating = CountDownLatch(1)
            val proceed = CountDownLatch(1)
            val job =
                launch(Dispatchers.IO) {
                    downloadWithCronet(file(), {}) { callback, executor ->
                        creating.countDown()
                        proceed.await()
                        fake.create(callback, executor)
                    }
                }
            creating.awaitEvent()
            job.cancelAndJoin()
            proceed.countDown()
            fake.started.awaitEvent()
            fake.cancelled.awaitEvent()
        }

    @Test fun timeoutWhileReadingCancelsRequestAndRemovesPartialFile() =
        runBlocking {
            val fake = FakeCronet()
            val target = file()
            val job =
                launch(Dispatchers.IO) {
                    try {
                        withTimeout(1_500) { downloadWithCronet(target, {}, fake::create) }
                    } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                        // Expected: the caller's per-attempt timeout cancels the request.
                    }
                }
            fake.started.awaitEvent()
            fake.response()
            fake.reading.awaitEvent()
            assertTrue(target.exists())
            job.join()
            fake.cancelled.awaitEvent()
            assertFalse(target.exists())
        }

    @Test fun responseIsStreamedAndProgressReported() =
        runBlocking {
            val fake = FakeCronet()
            `when`(fake.info.allHeaders).thenReturn(mapOf("Content-Length" to listOf("3")))
            val target = file()
            val progress = mutableListOf<Float?>()
            val job = async(Dispatchers.IO) { downloadWithCronet(target, { progress.add(it) }, fake::create) }
            fake.started.awaitEvent()
            fake.response()
            fake.reading.awaitEvent()
            fake.deliver("mp3")
            job.await()
            assertEquals("mp3", target.readText())
            assertEquals(listOf(1f), progress)
        }

    @Test fun httpErrorRetainsStatusForRetryPolicy() =
        runBlocking {
            val fake = FakeCronet()
            `when`(fake.info.httpStatusCode).thenReturn(404)
            val job =
                async(Dispatchers.IO) {
                    try {
                        downloadWithCronet(file(), {}, fake::create)
                        null
                    } catch (failure: IOException) {
                        failure
                    }
                }
            fake.started.awaitEvent()
            fake.response()
            assertEquals(404, (job.await() as VocarooHttpException).code)
            fake.cancelled.awaitEvent()
        }

    @Test fun requestSetupErrorsAreReported() =
        runBlocking {
            val errors =
                listOf(
                    IllegalArgumentException("invalid request"),
                    IllegalStateException("invalid state"),
                    SecurityException("permission denied"),
                    RejectedExecutionException("executor rejected"),
                )
            errors.forEach { cause ->
                val result =
                    try {
                        withTimeout(3_000) { downloadWithCronet(file(), {}) { _, _ -> throw cause } }
                        null
                    } catch (failure: IOException) {
                        failure
                    }
                assertTrue(generateSequence(result as Throwable?) { it.cause }.any { it === cause })
            }
        }

    @Test fun truncatedBodyIsRejected() =
        runBlocking {
            val fake = FakeCronet()
            `when`(fake.info.allHeaders).thenReturn(mapOf("Content-Length" to listOf("5")))
            val job =
                async(Dispatchers.IO) {
                    try {
                        downloadWithCronet(file(), {}, fake::create)
                        null
                    } catch (failure: IOException) {
                        failure
                    }
                }
            fake.started.awaitEvent()
            fake.response()
            fake.reading.awaitEvent()
            fake.deliver("mp3")
            assertTrue(job.await()?.message?.contains("Truncated") == true)
        }
}
