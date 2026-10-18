package com.solthariel.pawchan.data.network

import java.io.ByteArrayOutputStream

internal fun cleanJpeg(content: ByteArray): ByteArray {
    val output = ByteArrayOutputStream(content.size)
    output.write(content, 0, 2)
    var position = 2
    var ended = false
    while (position + 1 < content.size && !ended) {
        require(content[position].toInt() and BYTE_MASK == JPEG_MARKER)
        val marker = content[position + 1].toInt() and BYTE_MASK
        when (marker) {
            JPEG_MARKER -> {
                output.write(JPEG_MARKER)
                position += 1
            }

            JPEG_END -> {
                output.write(content, position, 2)
                ended = true
            }

            else -> {
                val length = jpegSegmentBytes(content, position)
                if (marker !in JPEG_PERSONAL_METADATA) output.write(content, position, length)
                position += length
                if (marker == JPEG_SCAN) {
                    val scanEnd = jpegScanEnd(content, position)
                    output.write(content, position, scanEnd - position)
                    position = scanEnd
                }
            }
        }
    }
    require(ended)
    return output.toByteArray()
}

private fun jpegSegmentBytes(
    content: ByteArray,
    position: Int,
): Int {
    require(position + JPEG_LENGTH_LOW_OFFSET < content.size)
    val length =
        ((content[position + 2].toInt() and BYTE_MASK) shl BITS_PER_BYTE) +
            (content[position + JPEG_LENGTH_LOW_OFFSET].toInt() and BYTE_MASK)
    require(length >= 2 && length <= content.size - position - 2)
    return length + 2
}

private fun jpegScanEnd(
    content: ByteArray,
    start: Int,
): Int {
    var position = start
    while (position + 1 < content.size) {
        if (content[position].toInt() and BYTE_MASK == JPEG_MARKER) {
            var markerOffset = position + 1
            while (markerOffset < content.size && content[markerOffset].toInt() and BYTE_MASK == JPEG_MARKER) {
                markerOffset += 1
            }
            require(markerOffset < content.size)
            val marker = content[markerOffset].toInt() and BYTE_MASK
            if (marker != 0 && marker !in JPEG_RESTART_START..JPEG_RESTART_END) return position
            position = markerOffset
        }
        position += 1
    }
    error("JPEG не содержит завершающего маркера")
}

// Keep JFIF, ICC and Adobe colour-transform segments; discard EXIF/XMP/IPTC/comments, even between scans.
private val JPEG_PERSONAL_METADATA = setOf(JPEG_EXIF_XMP, JPEG_IPTC, JPEG_COMMENT)
private const val BYTE_MASK = 0xff
private const val JPEG_MARKER = 0xff
private const val JPEG_SCAN = 0xda
private const val JPEG_END = 0xd9
private const val JPEG_LENGTH_LOW_OFFSET = 3
private const val BITS_PER_BYTE = 8
private const val JPEG_EXIF_XMP = 0xe1
private const val JPEG_IPTC = 0xed
private const val JPEG_COMMENT = 0xfe
private const val JPEG_RESTART_START = 0xd0
private const val JPEG_RESTART_END = 0xd7
