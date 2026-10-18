package com.solthariel.pawchan

import com.solthariel.pawchan.media.copyCancellableBytes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class CancellableStreamCopyTest {
    @Test
    fun preservesAllBytesAndReturnsActualLength() =
        runTest {
            val bytes = ByteArray(20_000) { it.toByte() }
            val output = ByteArrayOutputStream()
            assertEquals(bytes.size.toLong(), copyCancellableBytes(ByteArrayInputStream(bytes), output))
            assertArrayEquals(bytes, output.toByteArray())
        }

    @Test
    fun exactLimitIsAcceptedAndOverLimitIsRejectedBeforeWriting() =
        runTest {
            val output = ByteArrayOutputStream()
            assertEquals(2L, copyCancellableBytes(ByteArrayInputStream(byteArrayOf(1, 2)), output, 2))
            val rejected = ByteArrayOutputStream()
            try {
                copyCancellableBytes(ByteArrayInputStream(byteArrayOf(1, 2, 3)), rejected, 2)
                throw AssertionError("Byte limit ignored")
            } catch (_: IOException) {
                assertEquals(0, rejected.size())
            }
        }

    @Test
    fun cancellationDuringReadPreventsPublishingTheReadChunk() =
        runTest {
            val output = ByteArrayOutputStream()
            val read = CompletableDeferred<Unit>()
            var cancelled = false
            val owner =
                launch {
                    val input =
                        object : InputStream() {
                            override fun read(): Int = error("Bulk read required")

                            override fun read(buffer: ByteArray): Int {
                                read.complete(Unit)
                                coroutineContext[kotlinx.coroutines.Job]?.cancel()
                                buffer[0] = 1
                                return 1
                            }
                        }
                    try {
                        copyCancellableBytes(input, output)
                    } finally {
                        cancelled = true
                    }
                }
            read.await()
            owner.cancelAndJoin()
            assertTrue(cancelled)
            assertEquals(0, output.size())
        }
}
