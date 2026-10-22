package com.solthariel.pawchan.update

import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class UpdateTransferTest {
    @Test fun exactLengthCopiesAndReportsCompletion() {
        val output = ByteArrayOutputStream()
        val progress = mutableListOf<Int>()
        assertEquals(3L, copyUpdateStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)), output, 3, progress::add) {})
        assertArrayEquals(byteArrayOf(1, 2, 3), output.toByteArray())
        assertEquals(listOf(100), progress)
    }

    @Test fun truncatedBodyCannotReportSuccess() {
        assertThrows(IllegalStateException::class.java) {
            copyUpdateStream(ByteArrayInputStream(byteArrayOf(1)), ByteArrayOutputStream(), 2, {}) {}
        }
    }

    @Test fun oversizedChunkIsRejectedBeforeItIsWritten() {
        val output = ByteArrayOutputStream()
        assertThrows(IllegalStateException::class.java) {
            copyUpdateStream(ByteArrayInputStream(ByteArray(20)), output, 2, {}) {}
        }
        assertEquals(0, output.size())
    }

    @Test fun cancellationBeforeReadDoesNotWriteAnything() {
        val output = ByteArrayOutputStream()
        assertThrows(CancellationException::class.java) {
            copyUpdateStream(ByteArrayInputStream(byteArrayOf(1)), output, 1, {}) { throw CancellationException() }
        }
        assertEquals(0, output.size())
    }

    @Test fun cancellationAfterReadDoesNotPublishTheChunk() {
        val output = ByteArrayOutputStream()
        var checkpoints = 0
        assertThrows(CancellationException::class.java) {
            copyUpdateStream(ByteArrayInputStream(byteArrayOf(1)), output, 1, {}) {
                if (++checkpoints == 2) throw CancellationException()
            }
        }
        assertEquals(0, output.size())
    }

    @Test fun invalidDeclaredLengthsAreRejected() {
        listOf(0L, -1L, MAX_APK_BYTES + 1).forEach { length ->
            assertThrows(IllegalArgumentException::class.java) {
                copyUpdateStream(ByteArrayInputStream(byteArrayOf(1)), ByteArrayOutputStream(), length, {}) {}
            }
        }
    }
}
