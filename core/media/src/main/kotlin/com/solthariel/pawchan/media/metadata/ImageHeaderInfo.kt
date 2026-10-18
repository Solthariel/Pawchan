package com.solthariel.pawchan.media.metadata

/** Сведения из заголовков файла, которых нет у BitmapFactory. */
internal data class ImageHeaderInfo(
    val frames: Int? = null,
    val bitDepth: Int? = null,
    val colorType: String? = null,
    val compression: String? = null,
)

internal fun imageHeaderInfo(bytes: ByteArray): ImageHeaderInfo =
    when {
        bytes.startsWith(PNG_SIGNATURE) -> pngInfo(bytes)
        bytes.startsWith(GIF_SIGNATURE) -> ImageHeaderInfo(frames = gifFrames(bytes))
        bytes.startsWith(RIFF_SIGNATURE) && bytes.matches(WEBP_OFFSET, WEBP_SIGNATURE) -> webpInfo(bytes)
        else -> ImageHeaderInfo()
    }

private fun pngInfo(bytes: ByteArray): ImageHeaderInfo {
    if (bytes.size < PNG_IHDR_END) return ImageHeaderInfo()
    val bitDepth = bytes.unsigned(PNG_BIT_DEPTH)
    val colorType =
        when (bytes.unsigned(PNG_COLOR_TYPE)) {
            0 -> "оттенки серого"
            2 -> "RGB"
            PNG_PALETTE -> "палитра"
            PNG_GRAY_ALPHA -> "оттенки серого + альфа"
            PNG_RGBA -> "RGBA"
            else -> null
        }
    return ImageHeaderInfo(frames = apngFrames(bytes), bitDepth = bitDepth, colorType = colorType)
}

/** acTL стоит до первого IDAT; дальше искать незачем. */
private fun apngFrames(bytes: ByteArray): Int? {
    var offset = PNG_SIGNATURE.size
    var frames: Int? = null
    var searching = true
    while (searching && offset + CHUNK_HEADER + FOURCC <= bytes.size) {
        val length = bytes.intBigEndian(offset)
        if (bytes.matches(offset + FOURCC, ACTL)) frames = bytes.intBigEndian(offset + CHUNK_HEADER)
        searching = frames == null && length >= 0 && !bytes.matches(offset + FOURCC, IDAT)
        offset += CHUNK_HEADER + length + PNG_CRC
    }
    return frames
}

/** Считает кадры GIF, проходя блоки файла; у обрезанного файла — сколько успели найти. */
internal fun gifFrames(bytes: ByteArray): Int? {
    if (bytes.size < GIF_HEADER) return null
    return GifWalker(bytes).count().takeIf { it > 0 }
}

private class GifWalker(
    private val bytes: ByteArray,
) {
    private var offset = GIF_HEADER
    private var frames = 0

    fun count(): Int {
        val packed = bytes.unsigned(GIF_SCREEN_PACKED)
        if (packed and COLOR_TABLE_FLAG != 0) offset += colorTableSize(packed)
        var more = true
        while (more) more = offset in bytes.indices && step()
        return frames
    }

    private fun step(): Boolean =
        when (bytes.unsigned(offset)) {
            GIF_IMAGE -> image()
            GIF_EXTENSION -> skipSubBlocks(offset + 2)
            else -> false
        }

    private fun image(): Boolean {
        if (offset + GIF_DESCRIPTOR >= bytes.size) return false
        val local = bytes.unsigned(offset + GIF_DESCRIPTOR - 1)
        offset += GIF_DESCRIPTOR
        if (local and COLOR_TABLE_FLAG != 0) offset += colorTableSize(local)
        frames++
        return skipSubBlocks(offset + 1)
    }

    private fun skipSubBlocks(start: Int): Boolean {
        offset = start
        while (offset in bytes.indices) {
            val size = bytes.unsigned(offset)
            offset += 1 + size
            if (size == 0) return true
        }
        return false
    }
}

private fun colorTableSize(packed: Int): Int = RGB * (1 shl ((packed and COLOR_TABLE_BITS) + 1))

private fun webpInfo(bytes: ByteArray): ImageHeaderInfo {
    var offset = WEBP_CHUNKS
    var frames = 0
    var compression: String? = null
    while (offset + CHUNK_HEADER <= bytes.size) {
        val length = bytes.intBigEndian(offset + FOURCC, bigEndian = false)
        if (length < 0) break
        when {
            bytes.matches(offset, ANMF) -> frames++
            bytes.matches(offset, VP8L) -> compression = compression ?: "без потерь"
            bytes.matches(offset, VP8) -> compression = compression ?: "с потерями"
        }
        offset += CHUNK_HEADER + length + (length and 1)
    }
    return ImageHeaderInfo(frames = frames.takeIf { it > 0 }, compression = compression)
}

private fun ByteArray.startsWith(prefix: ByteArray): Boolean = matches(0, prefix)

private fun ByteArray.matches(
    offset: Int,
    expected: ByteArray,
): Boolean = offset >= 0 && offset + expected.size <= size && expected.indices.all { this[offset + it] == expected[it] }

private fun ByteArray.unsigned(offset: Int): Int = this[offset].toInt() and BYTE_MASK

private fun ByteArray.intBigEndian(
    offset: Int,
    bigEndian: Boolean = true,
): Int {
    val order = if (bigEndian) 0 until FOURCC else FOURCC - 1 downTo 0
    return order.fold(0) { value, index -> (value shl BYTE_BITS) or unsigned(offset + index) }
}

private fun ascii(text: String): ByteArray = text.toByteArray(Charsets.US_ASCII)

private val PNG_SIGNATURE = "\u0089PNG\r\n\u001A\n".toByteArray(Charsets.ISO_8859_1)
private val GIF_SIGNATURE = ascii("GIF8")
private val RIFF_SIGNATURE = ascii("RIFF")
private val WEBP_SIGNATURE = ascii("WEBP")
private val ACTL = ascii("acTL")
private val IDAT = ascii("IDAT")
private val ANMF = ascii("ANMF")
private val VP8L = ascii("VP8L")
private val VP8 = ascii("VP8 ")
private const val FOURCC = 4
private const val CHUNK_HEADER = 8
private const val PNG_CRC = 4
private const val PNG_BIT_DEPTH = 24
private const val PNG_COLOR_TYPE = 25
private const val PNG_IHDR_END = 26
private const val PNG_PALETTE = 3
private const val PNG_GRAY_ALPHA = 4
private const val PNG_RGBA = 6
private const val WEBP_OFFSET = 8
private const val WEBP_CHUNKS = 12
private const val GIF_HEADER = 13
private const val GIF_SCREEN_PACKED = 10
private const val GIF_DESCRIPTOR = 10
private const val GIF_IMAGE = 0x2C
private const val GIF_EXTENSION = 0x21
private const val COLOR_TABLE_FLAG = 0x80
private const val COLOR_TABLE_BITS = 0x07
private const val RGB = 3
private const val BYTE_MASK = 0xFF
private const val BYTE_BITS = 8
