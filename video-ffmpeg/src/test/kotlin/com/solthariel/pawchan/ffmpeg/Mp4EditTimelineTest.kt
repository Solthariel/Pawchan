package com.solthariel.pawchan.ffmpeg

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class Mp4EditTimelineTest {
    @Test fun sharedGapIsRemovedWithoutChangingTrackAlignmentOrSampleData() {
        val original = clip().readBytes()
        val result = viewBytes(original)
        val first = original.marker("elst")
        val second = original.marker("elst", first + 4)
        val src = ByteBuffer.wrap(original)
        val clip = ByteBuffer.wrap(result)
        val duration = original.marker("mvhd") + 20
        val firstGap = first + 12
        val secondGap = second + 12
        val gap = src.getInt(secondGap)
        assertTrue(gap > 500_000)
        assertEquals(src.getInt(duration) - gap, clip.getInt(duration))
        assertEquals(0, clip.getInt(secondGap))
        assertEquals(src.getInt(firstGap) - gap, clip.getInt(firstGap))
        assertTrue(clip.getInt(firstGap) in 1..100)
        val movie = original.marker("moov")
        assertArrayEquals(original.copyOfRange(0, movie), result.copyOfRange(0, movie))
        assertEquals(original.size, result.size)
    }

    @Test fun editsAreStableAcrossArbitrarySeeksAndReadsInsideASingleField() {
        val original = clip().readBytes()
        val expected = viewBytes(original)
        val provider = MpvProgressiveProvider(clip(), null, Runnable {}, Runnable {})
        try {
            repeat(2) {
                val stream = checkNotNull(provider.open("pawvideo://video"))
                try {
                    val first = original.marker("elst") + 12
                    for (position in (first - 3..first + 5).reversed()) {
                        assertEquals(position.toLong(), stream.seek(position.toLong()))
                        val byte = ByteBuffer.allocate(1)
                        assertEquals(1, stream.read(byte))
                        assertEquals(expected[position], byte.array().single())
                    }
                } finally {
                    stream.close()
                }
            }
            assertArrayEquals(original, clip().readBytes())
        } finally {
            provider.close()
        }
    }

    @Test fun ordinaryFilesAndZeroOriginAreByteIdentical() {
        for (bytes in listOf(clip("h264_aac.mp4").readBytes(), "ordinary webm or other input".toByteArray())) {
            assertArrayEquals(bytes, viewBytes(bytes))
        }
        val normalized = viewBytes(clip().readBytes())
        assertArrayEquals(normalized, viewBytes(normalized))
    }

    @Test fun complexEditsFragmentationAndStaggeredTracksAreNeverAdjusted() {
        val src = clip().readBytes()
        val first = src.marker("elst")
        val second = src.marker("elst", first + 4)
        val movie = src.marker("mvhd")
        val unknown = src.marker("udta")
        val changes =
            listOf<(ByteBuffer) -> Unit>(
                { it.putInt(first + 8, 3) },
                { it.putInt(first + 20, 0x00020000) },
                { it.putInt(second + 12, 0) },
                { it.putInt(movie + 16, 0) },
                { it.putInt(movie + 20, Int.MAX_VALUE) },
                { it.putInt(unknown, ByteBuffer.wrap("mvex".toByteArray()).int) },
            )
        for (change in changes) {
            val bytes = src.copyOf()
            change(ByteBuffer.wrap(bytes))
            assertArrayEquals(bytes, viewBytes(bytes))
        }
    }

    @Test fun truncatedMetadataOrInvalidBoxSizesPassThroughWithoutBlindReads() {
        val src = clip().readBytes()
        val movie = src.marker("moov")
        for (size in listOf(1, 7, 15, src.size - 10)) {
            val truncated = src.copyOf(size)
            assertArrayEquals(truncated, viewBytes(truncated))
        }
        val huge = src.copyOf()
        ByteBuffer.wrap(huge).putInt(movie - 4, Int.MAX_VALUE)
        assertArrayEquals(huge, viewBytes(huge))
    }

    private fun clip(name: String = "synthetic-common-gap.mp4"): File {
        val relative = "core/media/src/androidTest/assets/video/$name"
        return listOf(File(relative), File("../$relative")).first { it.isFile }
    }

    private fun ByteArray.marker(
        name: String,
        start: Int = 0,
    ): Int = toString(Charsets.ISO_8859_1).indexOf(name, start).also { require(it >= 0) }

    private fun viewBytes(original: ByteArray): ByteArray {
        val file = File.createTempFile("mpv-timeline-", ".bin")
        try {
            file.writeBytes(original)
            MpvProgressiveProvider(file, null, Runnable {}, Runnable {}).use { provider ->
                val stream = checkNotNull(provider.open("pawvideo://video"))
                try {
                    val bytes = ByteBuffer.allocate(original.size)
                    while (bytes.hasRemaining()) assertTrue(stream.read(bytes) > 0)
                    assertEquals(0, stream.read(ByteBuffer.allocate(1)))
                    return bytes.array()
                } finally {
                    stream.close()
                }
            }
        } finally {
            file.delete()
        }
    }
}
