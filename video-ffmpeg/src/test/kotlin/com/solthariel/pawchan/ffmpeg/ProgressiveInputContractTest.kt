package com.solthariel.pawchan.ffmpeg

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Contract applies to the progressive reader used by mpv. */
class ProgressiveInputContractTest {
    @Test fun independentReaderReceivesOnlyWrittenBytesAndCanRebind() {
        val payload = "example video bytes".toByteArray()
        val complete = CountDownLatch(1)
        val published = AtomicReference<Pair<Long, Long>>()
        val receiver =
            sink { prefix, total ->
                published.set(prefix to total)
                if (prefix == payload.size.toLong()) complete.countDown()
            }
        val directory = Files.createTempDirectory("paw-progressive-test").toFile()
        val source = source(directory, responseClient(200, payload))
        try {
            source.bind(receiver)
            source.start()
            assertTrue("completed publication", complete.await(5, TimeUnit.SECONDS))
            assertEquals(payload.size.toLong() to payload.size.toLong(), published.get())
            assertArrayEquals(payload, checkNotNull(source.file).readBytes())
            val rebound = AtomicReference<Pair<Long, Long>>()
            source.bind(sink { prefix, total -> rebound.set(prefix to total) })
            source.onReadReady()
            assertEquals(payload.size.toLong() to payload.size.toLong(), rebound.get())
        } finally {
            source.close()
            source.deleteFile()
            directory.delete()
        }
    }

    @Test fun detachedOrClosedSourceDoesNotRepublish() {
        val directory = Files.createTempDirectory("paw-progressive-test").toFile()
        val source = source(directory, responseClient(200, byteArrayOf(1)))
        val calls = AtomicInteger()
        val receiver = sink { _, _ -> calls.incrementAndGet() }
        try {
            source.bind(receiver)
            source.bind(null)
            source.onReadReady()
            assertEquals(0, calls.get())
            source.bind(receiver)
            source.close()
            source.onReadReady()
            assertEquals(0, calls.get())
        } finally {
            source.close()
            source.deleteFile()
            directory.delete()
        }
    }

    @Test fun httpErrorIsReportedAndDoesNotPublishBytes() {
        val directory = Files.createTempDirectory("paw-progressive-test").toFile()
        val failure = CountDownLatch(1)
        val calls = AtomicInteger()
        val source = source(directory, responseClient(404, byteArrayOf()), failure)
        try {
            source.bind(sink { _, _ -> calls.incrementAndGet() })
            source.start()
            assertTrue("HTTP failure", failure.await(5, TimeUnit.SECONDS))
            assertFalse(source.isComplete)
            assertTrue(source.failed)
            assertEquals(0, calls.get())
        } finally {
            source.close()
            source.deleteFile()
            directory.delete()
        }
    }

    private fun sink(onPublish: (Long, Long) -> Unit): ProgressiveInputSink =
        object : ProgressiveInputSink {
            override fun publish(
                prefix: Long,
                total: Long,
            ) = onPublish(prefix, total)

            override fun publishTail(
                start: Long,
                end: Long,
            ) = Unit
        }

    private fun responseClient(
        code: Int,
        payload: ByteArray,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                Response
                    .Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("test")
                    .body(payload.toResponseBody())
                    .build()
            }.build()

    private fun source(
        directory: File,
        client: OkHttpClient,
        failure: CountDownLatch = CountDownLatch(1),
    ) = ProgressiveSource(
        client,
        directory,
        "https://example.test/video",
        object : ProgressiveSource.Listener {
            override fun onFile(file: File) = Unit

            override fun onError(error: IOException) {
                failure.countDown()
            }
        },
    )
}
