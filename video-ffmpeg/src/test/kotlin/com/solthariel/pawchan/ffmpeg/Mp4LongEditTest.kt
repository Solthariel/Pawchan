package com.solthariel.pawchan.ffmpeg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class Mp4LongEditTest {
    @Test fun handles64BitDurationWithoutChangingFieldWidth() {
        val movie = longMovie(5_000_000_000L, 10_000L)
        val view = movie.copyOf()
        val overlays = mp4ClipTimeline(movie, 0)
        assertEquals(3, overlays.size)
        for (field in overlays) {
            assertEquals(Long.SIZE_BYTES, field.bytes.size)
            field.bytes.copyInto(view, field.offset.toInt())
        }
        assertEquals(0L, ByteBuffer.wrap(view).getLong(overlays.last().offset.toInt()))
        assertTrue(overlays.take(2).all { ByteBuffer.wrap(it.bytes).long == 10_000L })
        assertTrue(mp4ClipTimeline(view, 0).isEmpty())
    }

    @Test fun doesNotDropShortOrNonDominantDelays() {
        assertTrue(mp4ClipTimeline(longMovie(29_999, 1000), 0).isEmpty())
        assertTrue(mp4ClipTimeline(longMovie(60_000, 60_000), 0).isEmpty())
    }

    private fun longMovie(
        gap: Long,
        media: Long,
    ): ByteArray {
        val movie =
            ByteBuffer.allocate(32).apply {
                putInt(0, 0x01000000)
                putInt(20, 1000)
                putLong(24, gap + media)
            }
        val track =
            ByteBuffer.allocate(36).apply {
                putInt(0, 0x01000001)
                putLong(28, gap + media)
            }
        val edits =
            ByteBuffer
                .allocate(48)
                .putInt(0x01000000)
                .putInt(2)
                .putLong(gap)
                .putLong(-1)
                .putInt(0x10000)
                .putLong(media)
                .putLong(0)
                .putInt(0x10000)
        val handler = ByteArray(8) + "vide".toByteArray()
        val trak = atom("tkhd", track.array())
        val edts = atom("edts", atom("elst", edits.array()))
        val mdia = atom("mdia", atom("hdlr", handler))
        return atom("mvhd", movie.array()) + atom("trak", trak + edts + mdia)
    }

    private fun atom(
        name: String,
        bytes: ByteArray,
    ): ByteArray =
        ByteBuffer
            .allocate(bytes.size + 8)
            .putInt(bytes.size + 8)
            .put(name.toByteArray())
            .put(bytes)
            .array()
}
