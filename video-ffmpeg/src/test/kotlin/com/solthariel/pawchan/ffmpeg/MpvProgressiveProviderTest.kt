package com.solthariel.pawchan.ffmpeg

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class MpvProgressiveProviderTest {
    @Test fun fileSeekEofAndSingleReader() {
        val file = File.createTempFile("paw-mpv-test", ".bin")
        file.writeBytes(byteArrayOf(1, 2, 3, 4))
        val provider = MpvProgressiveProvider(file, null, Runnable {}, Runnable {})
        try {
            val stream = checkNotNull(provider.open("paw://video"))
            assertNull(provider.open("paw://video"))
            assertEquals(4L, stream.size())
            val buffer = ByteBuffer.allocate(3)
            assertEquals(3, stream.read(buffer))
            assertArrayEquals(byteArrayOf(1, 2, 3), buffer.array())
            assertEquals(2L, stream.seek(2))
            assertEquals(2, stream.read(ByteBuffer.allocate(4)))
            assertEquals(0, stream.read(ByteBuffer.allocate(1)))
            assertEquals(-1L, stream.seek(5))
            stream.close()
            checkNotNull(provider.open("paw://video")).close()
        } finally {
            provider.close()
            file.delete()
        }
        assertNull(provider.open("paw://video"))
    }

    @Test fun actualProgressiveSourceFeedsMpvReader() {
        val bytes = "network video bytes".toByteArray()
        val ready = CountDownLatch(1)
        val directory = Files.createTempDirectory("paw-mpv-network").toFile()
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
                        .body(bytes.toResponseBody())
                        .build()
                }.build()
        val source =
            ProgressiveSource(
                client,
                directory,
                "https://example.test/video",
                object : ProgressiveSource.Listener {
                    override fun onFile(file: File) {
                        ready.countDown()
                    }

                    override fun onError(error: IOException) = Unit
                },
            )
        try {
            source.start()
            assertTrue(ready.await(5, TimeUnit.SECONDS))
            val provider = MpvProgressiveProvider(checkNotNull(source.file), source, Runnable {}, Runnable {})
            val stream = checkNotNull(provider.open("paw://video"))
            val buffer = ByteBuffer.allocate(bytes.size)
            var total = 0
            while (total < bytes.size) {
                val n = stream.read(buffer)
                assertTrue(n > 0)
                total += n
            }
            assertArrayEquals(bytes, buffer.array())
            stream.close()
            provider.close()
        } finally {
            source.close()
            source.deleteFile()
            directory.delete()
        }
    }

    @Test fun cancellationUnblocksReaderWaitingForPublishedBytes() {
        val file = File.createTempFile("paw-mpv-wait", ".bin")
        val waiting = CountDownLatch(1)
        val input = PublishedInput(Runnable { waiting.countDown() }, Runnable {})
        val callback =
            object : ProgressiveReadCallback {
                override fun onReadReady() = Unit

                override fun requestPartFromPosition(position: Long) = Unit
            }
        input.open(file, callback)
        val stream = MpvPublishedStream(input) {}
        val result = AtomicReference<Int>()
        val worker = thread { result.set(stream.read(ByteBuffer.allocate(1))) }
        try {
            assertTrue(waiting.await(5, TimeUnit.SECONDS))
            stream.cancel()
            worker.join(2000)
            assertTrue(!worker.isAlive)
            assertEquals(-1, result.get())
        } finally {
            stream.cancel()
            worker.join(2000)
            stream.close()
            file.delete()
        }
    }
}
