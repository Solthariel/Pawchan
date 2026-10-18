package com.solthariel.pawchan.data.network

internal fun uniqueGif(
    content: ByteArray,
    nonce: ByteArray,
): ByteArray {
    require(content.size > GIF_HEADER_BYTES && content.last() == GIF_TRAILER)
    val packed = content[GIF_PACKED_OFFSET].toInt() and BYTE_MASK
    val tableBytes =
        if (packed and GIF_GLOBAL_TABLE_FLAG != 0) {
            GIF_COLOR_BYTES * (1 shl ((packed and GIF_TABLE_SIZE_MASK) + 1))
        } else {
            0
        }
    val insertion = GIF_HEADER_BYTES + tableBytes
    require(insertion < content.size)
    // Insert before animation extensions/frames, not into the final frame's packet.
    val comment = byteArrayOf(GIF_EXTENSION, GIF_COMMENT, nonce.size.toByte()) + nonce + byteArrayOf(0)
    return content.copyOfRange(0, insertion) + comment + content.copyOfRange(insertion, content.size)
}

private const val GIF_HEADER_BYTES = 13
private const val GIF_PACKED_OFFSET = 10
private const val GIF_GLOBAL_TABLE_FLAG = 0x80
private const val GIF_TABLE_SIZE_MASK = 0x07
private const val GIF_COLOR_BYTES = 3
private const val BYTE_MASK = 0xff
private const val GIF_EXTENSION: Byte = 0x21
private const val GIF_COMMENT: Byte = -2
private const val GIF_TRAILER: Byte = 0x3b
