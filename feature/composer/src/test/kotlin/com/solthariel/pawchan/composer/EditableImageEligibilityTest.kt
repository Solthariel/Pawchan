package com.solthariel.pawchan.composer

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class EditableImageEligibilityTest {
    @Test
    fun jpegPngAndStaticWebpAreAllowed() {
        assertTrue(isEditableStillImage(byteArrayOf(-1, -40, -1)))
        assertTrue(isEditableStillImage(png(animated = false)))
        assertTrue(isEditableStillImage(webp(animated = false)))
    }

    @Test
    fun animationAndTruncatedContainersAreRejected() {
        assertFalse(isEditableStillImage(png(animated = true)))
        assertFalse(isEditableStillImage(webp(animated = true)))
        val animationChunk = "ANIM".encodeToByteArray() + ByteArray(4)
        val animatedPayload = webp(animated = false).drop(8).toByteArray() + animationChunk
        val animatedWithoutFlag =
            "RIFF".encodeToByteArray() +
                byteArrayOf(animatedPayload.size.toByte(), 0, 0, 0) + animatedPayload
        assertFalse(isEditableStillImage(animatedWithoutFlag))
        assertFalse(isEditableStillImage(webp(animated = false).dropLast(1).toByteArray()))
        assertFalse(isEditableStillImage(png(animated = false).dropLast(1).toByteArray()))
        assertFalse(isEditableStillImage("GIF89a".encodeToByteArray()))
        assertFalse(isEditableStillImage(byteArrayOf()))
    }

    @Test fun completedPngWithTrailingHashBytesRemainsEditable() {
        assertTrue(isEditableStillImage(png(animated = false) + byteArrayOf(7, 9, 11)))
        assertFalse(isEditableStillImage(png(animated = true) + byteArrayOf(7, 9, 11)))
    }

    private fun png(animated: Boolean): ByteArray =
        ByteArrayOutputStream()
            .apply {
                write(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))
                write(pngChunk("IHDR", ByteArray(13)))
                if (animated) write(pngChunk("acTL", ByteArray(8)))
                write(pngChunk("IDAT", byteArrayOf(1)))
                write(pngChunk("IEND", byteArrayOf()))
            }.toByteArray()

    private fun pngChunk(
        type: String,
        data: ByteArray,
    ): ByteArray =
        ByteArrayOutputStream()
            .apply {
                write(byteArrayOf(0, 0, 0, data.size.toByte()))
                write(type.encodeToByteArray())
                write(data)
                write(ByteArray(4))
            }.toByteArray()

    private fun webp(animated: Boolean): ByteArray {
        val flags = if (animated) 2 else 0
        val payload =
            ByteArrayOutputStream()
                .apply {
                    write("WEBPVP8X".encodeToByteArray())
                    write(byteArrayOf(10, 0, 0, 0))
                    write(byteArrayOf(flags.toByte()) + ByteArray(9))
                    write("VP8 ".encodeToByteArray())
                    write(byteArrayOf(2, 0, 0, 0, 1, 2))
                }.toByteArray()
        return "RIFF".encodeToByteArray() + byteArrayOf(payload.size.toByte(), 0, 0, 0) + payload
    }
}
