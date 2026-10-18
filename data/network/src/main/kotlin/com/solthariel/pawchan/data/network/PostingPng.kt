package com.solthariel.pawchan.data.network

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

internal fun editPng(
    content: ByteArray,
    removeMetadata: Boolean,
    nonce: ByteArray?,
): ByteArray {
    val output = ByteArrayOutputStream(content.size)
    output.write(content, 0, PNG_SIGNATURE_LENGTH)
    var position = PNG_SIGNATURE_LENGTH
    var ended = false
    while (position + PNG_CHUNK_OVERHEAD <= content.size && !ended) {
        val length = ByteBuffer.wrap(content, position, PNG_LENGTH_BYTES).int
        require(length >= 0 && length <= content.size - position - PNG_CHUNK_OVERHEAD)
        val type = String(content, position + PNG_LENGTH_BYTES, PNG_TYPE_BYTES, Charsets.US_ASCII)
        if (type == "IEND") {
            require(length == 0)
            nonce?.let { output.write(pngChunk("npAD", it)) }
            ended = true
        }
        if (!removeMetadata || type !in PNG_METADATA) {
            output.write(content, position, length + PNG_CHUNK_OVERHEAD)
        }
        position += length + PNG_CHUNK_OVERHEAD
    }
    require(ended && position == content.size)
    return output.toByteArray()
}

private fun pngChunk(
    type: String,
    payload: ByteArray,
): ByteArray {
    val data = ascii(type) + payload
    val crc = CRC32().apply { update(data) }.value.toInt()
    return intBytes(payload.size, ByteOrder.BIG_ENDIAN) + data + intBytes(crc, ByteOrder.BIG_ENDIAN)
}

private val PNG_METADATA = setOf("tEXt", "zTXt", "iTXt", "eXIf", "tIME")
private const val PNG_SIGNATURE_LENGTH = 8
private const val PNG_CHUNK_OVERHEAD = 12

private const val PNG_LENGTH_BYTES = 4
private const val PNG_TYPE_BYTES = 4
