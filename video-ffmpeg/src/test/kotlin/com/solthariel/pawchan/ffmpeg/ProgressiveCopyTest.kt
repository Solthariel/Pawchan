package com.solthariel.pawchan.ffmpeg

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

class ProgressiveCopyTest {
    private val file = File.createTempFile("paw-copy", ".media")

    @After fun deleteFile() {
        file.delete()
    }

    @Test fun writesAtOffsetAndPublishesOnlyWrittenEnds() {
        val ends = mutableListOf<Pair<Long, Long>>()

        copyBody(ByteArrayInputStream(byteArrayOf(5, 6, 7)), file, 2, 5, { true }) { previous, end ->
            ends += previous to end
        }

        assertArrayEquals(byteArrayOf(0, 0, 5, 6, 7), file.readBytes())
        assertEquals(listOf(2L to 5L), ends)
    }

    @Test fun bodyLongerThanDeclaredIsRejected() {
        assertThrows(IOException::class.java) {
            copyBody(ByteArrayInputStream(byteArrayOf(1, 2, 3)), file, 0, 2, { true }) { _, _ -> }
        }
    }

    @Test fun truncatedBodyIsRejectedWhileLive() {
        assertThrows(IOException::class.java) {
            copyBody(ByteArrayInputStream(byteArrayOf(1, 2)), file, 0, 4, { true }) { _, _ -> }
        }
    }

    @Test fun cancelledCopyStopsQuietlyWithoutWritingMore() {
        var live = true
        val chunks = ChunkedStream(listOf(byteArrayOf(1, 2), byteArrayOf(3, 4)))

        copyBody(chunks, file, 0, 4, { live }) { _, _ -> live = false }

        assertArrayEquals(byteArrayOf(1, 2), file.readBytes())
    }

    @Test fun unknownLengthAcceptsAnyCompleteBody() {
        copyBody(ByteArrayInputStream(byteArrayOf(9, 8)), file, 0, -1, { true }) { _, _ -> }

        assertArrayEquals(byteArrayOf(9, 8), file.readBytes())
    }

    private class ChunkedStream(
        chunks: List<ByteArray>,
    ) : InputStream() {
        private val pending = ArrayDeque(chunks)

        override fun read(): Int = throw UnsupportedOperationException()

        override fun read(buffer: ByteArray): Int {
            val next = pending.removeFirstOrNull() ?: return -1
            next.copyInto(buffer)
            return next.size
        }
    }
}
