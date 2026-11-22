package com.solthariel.pawchan.ffmpeg

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ProgressiveSourceCleanupTest {
    @Test fun nativeReaderReleaseDoesNotWaitForAnUnfinishedWriter() {
        val directory = Files.createTempDirectory("paw-source-cleanup").toFile()
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
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
                        .body(ByteArray(64).toResponseBody())
                        .build()
                }.build()
        val source =
            ProgressiveSource(
                client,
                directory,
                "https://example.test/video",
                object : ProgressiveSource.Listener {
                    override fun onFile(file: File) {
                        started.countDown()
                        // Simulate a slow writer that has not yet left its callback.
                        while (release.count > 0L) {
                            try {
                                release.await()
                            } catch (_: InterruptedException) {
                                // Cancellation does not imply the I/O thread has terminated yet.
                            }
                        }
                    }

                    override fun onError(error: IOException) = Unit
                },
            )
        try {
            source.start()
            assertTrue(started.await(5, TimeUnit.SECONDS))
            val temporary = checkNotNull(source.file)
            assertTrue(temporary.exists())
            source.close()
            val begin = System.nanoTime()
            source.deleteFile()
            source.deleteFile() // idempotent even with a pending writer
            val duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin)
            assertTrue("Native teardown must not wait for the download ($duration ms)", duration < 1_000)
            assertTrue("A live writer still owns this file", temporary.exists())
            release.countDown()
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
            while (temporary.exists() && System.nanoTime() < deadline) Thread.sleep(10)
            assertFalse("The last writer must reclaim the file", temporary.exists())
        } finally {
            release.countDown()
            source.close()
            source.deleteFile()
            directory.delete()
        }
    }
}
