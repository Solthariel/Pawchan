package com.solthariel.pawchan.ffmpeg

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.ISO_8859_1

/** A range within a bounded copy of moov, never an index into an unverified sparse file. */
internal data class Mp4Box(
    val type: String,
    val start: Int,
    val end: Int,
)

internal fun ByteBuffer.boxes(
    start: Int,
    end: Int,
): List<Mp4Box> {
    val boxes = mutableListOf<Mp4Box>()
    var offset = start
    while (offset < end) {
        val remaining = end - offset
        require(remaining >= BOX_HEADER && boxes.size < MAX_CHILDREN)
        val size32 = Integer.toUnsignedLong(getInt(offset))
        val header = if (size32 == 1L) EXTENDED_HEADER else BOX_HEADER
        require(remaining >= header)
        val size =
            when (size32) {
                0L -> remaining.toLong()
                1L -> getLong(offset + BOX_HEADER)
                else -> size32
            }
        require(size in header.toLong()..remaining.toLong())
        val next = offset + size.toInt()
        boxes += Mp4Box(fourCc(offset + Int.SIZE_BYTES), offset + header, next)
        offset = next
    }
    return boxes
}

internal fun ByteBuffer.fourCc(offset: Int): String = String(array(), offset, Int.SIZE_BYTES, ISO_8859_1)

internal const val BOX_HEADER = 8
internal const val EXTENDED_HEADER = 16
private const val MAX_CHILDREN = 128
