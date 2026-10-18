package com.solthariel.pawchan.data.network

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.CRC32

class PostingMediaBytesTest {
    @Test
    fun pngUniqueHashAddsValidChunkAndKeepsOriginalAndImageBytes() {
        val original = png()
        val saved = original.copyOf()
        val first = PostingMediaBytes.unique(original)
        val second = PostingMediaBytes.unique(original)
        assertArrayEquals(saved, original)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(chunks(original).getValue("IDAT"), chunks(first).getValue("IDAT"))
        assertTrue("npAD" in chunks(first))
    }

    @Test
    fun pngMetadataRemovalPreservesColourAndPixels() {
        val original = png()
        val offset = original.size - 12
        val metadata = chunk("tEXt", "author\u0000private".toByteArray())
        val withMetadata = original.copyOfRange(0, offset) + metadata + original.copyOfRange(offset, original.size)
        assertArrayEquals(original, PostingMediaBytes.removeMetadata(withMetadata))
    }

    @Test
    fun jpegCleaningKeepsColourProfileAndCompressedImage() {
        val original =
            byteArrayOf(-1, -40) + byteArrayOf(-1, -31, 0, 5, 1, 2, 3) +
                byteArrayOf(-1, -30, 0, 4, 4, 5) + byteArrayOf(-1, -38, 0, 2, 6, -1, -39)
        val expected = byteArrayOf(-1, -40, -1, -30, 0, 4, 4, 5, -1, -38, 0, 2, 6, -1, -39)
        assertArrayEquals(expected, PostingMediaBytes.removeMetadata(original))
        assertEquals("jpg", PostingMediaBytes.format(PostingMediaBytes.unique(original)))
    }

    @Test
    fun jpegClearsMetadataBetweenScansWithoutRemovingStuffedOrRestartBytes() {
        val first = byteArrayOf(-1, -40, -1, -38, 0, 2, 6, -1, 0, -1, -48)
        val metadata = byteArrayOf(-1, -19, 0, 5, 1, 2, 3)
        val second = byteArrayOf(-1, -38, 0, 2, 7, -1, -39)
        val original = first + metadata + second + ascii("private-trailing-data")
        assertArrayEquals(first + second, PostingMediaBytes.removeMetadata(original))
    }

    @Test
    fun videoHashesUseContainerPaddingNotRandomTrailingGarbage() {
        val mp4 = intBytes(16, ByteOrder.BIG_ENDIAN) + ascii("ftyp") + ascii("isom") + byteArrayOf(0, 0, 0, 0)
        val edited = PostingMediaBytes.unique(mp4)
        assertArrayEquals(mp4, edited.copyOf(mp4.size))
        assertEquals("free", String(edited, mp4.size + 4, 4))
        assertEquals(24, ByteBuffer.wrap(edited, mp4.size, 4).int)
        val webm = byteArrayOf(0x1a, 0x45, -33, -93, -128)
        val webmEdited = PostingMediaBytes.unique(webm)
        assertArrayEquals(webm, webmEdited.copyOf(webm.size))
        assertEquals(0xec, webmEdited[webm.size].toInt() and 0xff)
    }

    @Test
    fun webpUpdatesRiffLengthAndGifKeepsTrailer() {
        val webp = ascii("RIFF") + intBytes(4, ByteOrder.LITTLE_ENDIAN) + ascii("WEBP")
        val edited = PostingMediaBytes.unique(webp)
        assertEquals(edited.size - 8, ByteBuffer.wrap(edited, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int)
        val gif = ascii("GIF89a") + byteArrayOf(1, 0, 1, 0, 0, 0, 0, 0x3b)
        assertEquals(0x3b, PostingMediaBytes.unique(gif).last().toInt())
    }

    @Test
    fun rejectsTruncatedContainersRatherThanSendingBrokenCopies() {
        assertThrows(IllegalArgumentException::class.java) { PostingMediaBytes.removeMetadata(png().copyOf(20)) }
        assertThrows(IllegalArgumentException::class.java) {
            PostingMediaBytes.removeMetadata(byteArrayOf(-1, -40, -1, 1))
        }
    }

    private fun png(): ByteArray =
        Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jF1kAAAAASUVORK5CYII=",
        )

    private fun chunk(
        type: String,
        bytes: ByteArray,
    ): ByteArray {
        val data = ascii(type) + bytes
        return intBytes(bytes.size, ByteOrder.BIG_ENDIAN) + data +
            intBytes(CRC32().apply { update(data) }.value.toInt(), ByteOrder.BIG_ENDIAN)
    }

    private fun chunks(bytes: ByteArray): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        var offset = 8
        while (offset < bytes.size) {
            val length = ByteBuffer.wrap(bytes, offset, 4).int
            val type = String(bytes, offset + 4, 4)
            val data = bytes.copyOfRange(offset + 4, offset + 8 + length)
            if (type == "npAD") {
                assertEquals(
                    ByteBuffer.wrap(bytes, offset + 8 + length, 4).int,
                    CRC32().apply { update(data) }.value.toInt(),
                )
            }
            result[type] = bytes.copyOfRange(offset + 8, offset + 8 + length)
            offset += length + 12
        }
        return result
    }
}
