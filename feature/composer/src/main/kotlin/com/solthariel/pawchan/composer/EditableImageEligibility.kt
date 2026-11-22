package com.solthariel.pawchan.composer

/** Reject animated/truncated image streams; decoders may ignore bytes after a complete PNG IEND. */
internal fun isEditableStillImage(bytes: ByteArray): Boolean =
    when {
        bytes.startsWith(JPEG_SIGNATURE) -> true
        bytes.startsWith(PNG_SIGNATURE) -> isStaticPng(bytes)
        bytes.startsWithAscii(0, "RIFF") && bytes.startsWithAscii(RIFF_PREFIX, "WEBP") -> isStaticWebp(bytes)
        else -> false
    }

private fun isStaticPng(bytes: ByteArray): Boolean {
    var offset = PNG_SIGNATURE.size
    var hasHeader = false
    var hasData = false
    var finished = false
    var invalid = false
    while (!finished && !invalid && offset <= bytes.size - CHUNK_OVERHEAD) {
        val size = bytes.readBigEndian(offset)
        if (size > bytes.size.toLong() - offset - CHUNK_OVERHEAD) {
            invalid = true
        } else {
            val type = offset + INTEGER_BYTES
            when {
                bytes.startsWithAscii(type, "IHDR") -> {
                    hasHeader = offset == PNG_SIGNATURE.size && size == PNG_HEADER_SIZE
                }

                bytes.startsWithAscii(type, "acTL") -> {
                    invalid = true
                }

                bytes.startsWithAscii(type, "IDAT") -> {
                    hasData = true
                }

                bytes.startsWithAscii(type, "IEND") -> {
                    invalid = size != 0L
                    finished = true
                }
            }
            offset += size.toInt() + CHUNK_OVERHEAD
        }
    }
    return !invalid && finished && hasHeader && hasData
}

private fun isStaticWebp(bytes: ByteArray): Boolean {
    var invalid = bytes.size < RIFF_HEADER || bytes.readLittleEndian(INTEGER_BYTES) != bytes.size.toLong() - RIFF_PREFIX
    var offset = RIFF_HEADER
    var hasImage = false
    while (!invalid && offset <= bytes.size - WEBP_CHUNK_HEADER) {
        val size = bytes.readLittleEndian(offset + INTEGER_BYTES)
        val padding = size % 2
        if (size + padding > bytes.size.toLong() - offset - WEBP_CHUNK_HEADER) {
            invalid = true
        } else {
            when {
                bytes.startsWithAscii(offset, "ANIM") || bytes.startsWithAscii(offset, "ANMF") -> {
                    invalid = true
                }

                bytes.startsWithAscii(offset, "VP8X") -> {
                    invalid = size != VP8X_SIZE || bytes[offset + WEBP_CHUNK_HEADER].toInt() and ANIMATION_FLAG != 0
                }

                bytes.startsWithAscii(offset, "VP8 ") || bytes.startsWithAscii(offset, "VP8L") -> {
                    hasImage = size > 0
                }
            }
            offset += WEBP_CHUNK_HEADER + (size + padding).toInt()
        }
    }
    return !invalid && hasImage && offset == bytes.size
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
    val enough = size >= prefix.size
    return enough && prefix.indices.all { this[it] == prefix[it] }
}

private fun ByteArray.startsWithAscii(
    offset: Int,
    value: String,
): Boolean {
    val enough = offset >= 0 && offset <= size - value.length
    return enough && value.indices.all { this[offset + it] == value[it].code.toByte() }
}

private fun ByteArray.readBigEndian(offset: Int): Long =
    (0 until INTEGER_BYTES).fold(0L) { value, index ->
        (value shl BITS_PER_BYTE) or (this[offset + index].toLong() and BYTE_MASK)
    }

private fun ByteArray.readLittleEndian(offset: Int): Long =
    (0 until INTEGER_BYTES).fold(0L) { value, index ->
        value or ((this[offset + index].toLong() and BYTE_MASK) shl (index * BITS_PER_BYTE))
    }

private val JPEG_SIGNATURE = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte())
private val PNG_SIGNATURE = byteArrayOf(0x89.toByte()) + "PNG\r\n\u001a\n".encodeToByteArray()
private const val INTEGER_BYTES = 4
private const val BITS_PER_BYTE = 8
private const val BYTE_MASK = 0xffL
private const val CHUNK_OVERHEAD = 12
private const val PNG_HEADER_SIZE = 13L
private const val RIFF_HEADER = 12
private const val RIFF_PREFIX = 8
private const val WEBP_CHUNK_HEADER = 8
private const val VP8X_SIZE = 10L
private const val ANIMATION_FLAG = 2
