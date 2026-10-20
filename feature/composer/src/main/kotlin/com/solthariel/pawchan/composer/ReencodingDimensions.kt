package com.solthariel.pawchan.composer

import kotlin.math.roundToInt

internal data class ReencodingDimensions(
    val width: Int,
    val height: Int,
) {
    fun oriented(orientation: Int): ReencodingDimensions =
        if (orientation in TRANSPOSED_ORIENTATIONS) ReencodingDimensions(height, width) else this
}

/** The preview and encoder use the same rounding and one-pixel minimum. EXIF is applied after resizing. */
internal fun reencodingDimensions(
    width: Int,
    height: Int,
    settings: ImageReencoding,
): ReencodingDimensions? =
    if (width > 0 && height > 0) {
        ReencodingDimensions(
            (width * settings.scaleFactor).roundToInt().coerceAtLeast(1),
            (height * settings.scaleFactor).roundToInt().coerceAtLeast(1),
        )
    } else {
        null
    }

private val TRANSPOSED_ORIENTATIONS = EXIF_TRANSPOSE..EXIF_ROTATE_270

private const val EXIF_TRANSPOSE = 5
private const val EXIF_ROTATE_270 = 8
