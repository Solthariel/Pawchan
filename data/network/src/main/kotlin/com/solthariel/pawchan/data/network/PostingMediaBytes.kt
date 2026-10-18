package com.solthariel.pawchan.data.network

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom

/** Container-level edits preserve compressed pixels/audio; callers work on upload copies. */
object PostingMediaBytes {
    fun unique(content: ByteArray): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        return when (format(content)) {
            "jpg" -> {
                content.copyOfRange(0, 2) + JPEG_COMMENT_MARKER + byteArrayOf(0, (nonce.size + 2).toByte()) +
                    nonce + content.copyOfRange(2, content.size)
            }

            "png" -> {
                editPng(content, false, nonce)
            }

            "gif" -> {
                uniqueGif(content, nonce)
            }

            "webp" -> {
                uniqueWebp(content, nonce)
            }

            "mp4" -> {
                content + intBytes(nonce.size + CONTAINER_BOX_HEADER, ByteOrder.BIG_ENDIAN) + ascii("free") + nonce
            }

            "webm" -> {
                content + byteArrayOf(EBML_VOID, (EBML_SINGLE_BYTE_SIZE + nonce.size).toByte()) + nonce
            }

            else -> {
                error("Этот формат не поддерживает изменение хеша без перекодирования")
            }
        }
    }

    fun removeMetadata(content: ByteArray): ByteArray =
        when (format(content)) {
            "jpg" -> cleanJpeg(content)
            "png" -> editPng(content, true, null)
            else -> error("Удаление метаданных поддерживается для JPEG и PNG")
        }

    fun format(content: ByteArray): String? =
        when {
            content.startsWithBytes(JPEG_SIGNATURE) -> "jpg"
            content.startsWithBytes(PNG_SIGNATURE) -> "png"
            content.startsWithBytes(ascii("GIF8")) -> "gif"
            isWebp(content) -> "webp"
            isMp4(content) -> "mp4"
            content.startsWithBytes(WEBM_SIGNATURE) -> "webm"
            else -> null
        }

    private fun uniqueWebp(
        content: ByteArray,
        nonce: ByteArray,
    ): ByteArray {
        val originalSize = ByteBuffer.wrap(content, INTEGER_BYTES, INTEGER_BYTES).order(ByteOrder.LITTLE_ENDIAN).int
        require(originalSize == content.size - CONTAINER_BOX_HEADER)
        val result = content + ascii("JUNK") + intBytes(nonce.size, ByteOrder.LITTLE_ENDIAN) + nonce
        intBytes(result.size - CONTAINER_BOX_HEADER, ByteOrder.LITTLE_ENDIAN).copyInto(result, INTEGER_BYTES)
        return result
    }
}

internal fun intBytes(
    value: Int,
    order: ByteOrder,
): ByteArray =
    ByteBuffer
        .allocate(INTEGER_BYTES)
        .order(order)
        .putInt(value)
        .array()

internal fun ascii(value: String): ByteArray = value.toByteArray(Charsets.US_ASCII)

private fun ByteArray.startsWithBytes(prefix: ByteArray): Boolean =
    size >= prefix.size &&
        prefix.indices.all { this[it] == prefix[it] }

private const val NONCE_BYTES = 16

private val JPEG_SIGNATURE = hexBytes("ffd8")
private val JPEG_COMMENT_MARKER = hexBytes("fffe")
private val PNG_SIGNATURE = hexBytes("89504e470d0a1a0a")
private val WEBM_SIGNATURE = hexBytes("1a45dfa3")
private const val EBML_VOID: Byte = -20
private const val EBML_SINGLE_BYTE_SIZE = 0x80
private const val INTEGER_BYTES = 4
private const val CONTAINER_BOX_HEADER = 8
private const val CONTAINER_SIGNATURE_END = 12

private fun hexBytes(value: String): ByteArray = value.chunked(2).map { it.toInt(HEX_RADIX).toByte() }.toByteArray()

private fun isWebp(content: ByteArray): Boolean =
    content.size >= CONTAINER_SIGNATURE_END && content.startsWithBytes(ascii("RIFF")) &&
        content.copyOfRange(CONTAINER_BOX_HEADER, CONTAINER_SIGNATURE_END).contentEquals(ascii("WEBP"))

private fun isMp4(content: ByteArray): Boolean =
    content.size >= CONTAINER_SIGNATURE_END &&
        content.copyOfRange(INTEGER_BYTES, CONTAINER_BOX_HEADER).contentEquals(ascii("ftyp"))

private const val HEX_RADIX = 16
