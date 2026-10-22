package com.solthariel.pawchan

import java.io.ByteArrayOutputStream

/** Binary image samples shared by metadata parser assertions. */
internal object MetadataImageFixtures {
    fun gif(frames: Int): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("GIF89a".toByteArray())
        // Логический экран 1×1 с глобальной палитрой из двух цветов.
        out.write(byteArrayOf(1, 0, 1, 0, 0x80.toByte(), 0, 0))
        out.write(ByteArray(6))
        repeat(frames) {
            out.write(byteArrayOf(0x21, 0xF9.toByte(), 4, 0, 10, 0, 0, 0))
            out.write(byteArrayOf(0x2C, 0, 0, 0, 0, 1, 0, 1, 0, 0))
            out.write(byteArrayOf(2, 2, 0x44, 0x01, 0))
        }
        out.write(0x3B)
        return out.toByteArray()
    }

    fun chunk(
        type: String,
        data: ByteArray,
    ): ByteArray = be(data.size) + type.toByteArray() + data + ByteArray(4)

    fun riff(
        type: String,
        data: ByteArray,
    ): ByteArray = type.toByteArray() + le(data.size) + data + ByteArray(data.size % 2)

    private fun be(value: Int) = ByteArray(4) { (value shr (24 - 8 * it)).toByte() }

    fun le(value: Int) = be(value).reversedArray()

    val PNG = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
}
