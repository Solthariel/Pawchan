package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

internal data class ImageEditBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

internal data class ImageEditDimensions(
    val crop: ImageEditBounds,
    val width: Int,
    val height: Int,
)

/** Dimensions passed here must be the actual EXIF-oriented decoded bitmap dimensions. */
internal fun imageEditDimensions(
    width: Int,
    height: Int,
    edit: DraftImageEdit,
    encoding: DraftImageReencoding?,
): ImageEditDimensions {
    require(width > 0 && height > 0)
    val crop = edit.crop
    val left = floor(crop.left.toDouble() * width).toInt().coerceIn(0, width - 1)
    val top = floor(crop.top.toDouble() * height).toInt().coerceIn(0, height - 1)
    val right = ceil(crop.right.toDouble() * width).toInt().coerceIn(left + 1, width)
    val bottom = ceil(crop.bottom.toDouble() * height).toInt().coerceIn(top + 1, height)
    val croppedWidth = right - left
    val croppedHeight = bottom - top
    val rotatedWidth = if (edit.quarterTurns % 2 == 0) croppedWidth else croppedHeight
    val rotatedHeight = if (edit.quarterTurns % 2 == 0) croppedHeight else croppedWidth
    val scale = encoding?.scaleFactor ?: 1f
    return ImageEditDimensions(
        ImageEditBounds(left, top, right, bottom),
        (rotatedWidth * scale).roundToInt().coerceAtLeast(1),
        (rotatedHeight * scale).roundToInt().coerceAtLeast(1),
    )
}

/** Choose a conservative power-of-two sample, including rounded-up decoder dimensions. */
internal fun imageEditDecodeSample(
    width: Int,
    height: Int,
    pixelLimit: Long = IMAGE_EDIT_DECODE_PIXEL_LIMIT,
): Int {
    require(width > 0 && height > 0 && pixelLimit > 0)
    var sample = 1
    while (ceilDiv(width, sample) * ceilDiv(height, sample) > pixelLimit) {
        require(sample <= Int.MAX_VALUE / 2) { "Невозможно выбрать безопасный размер изображения" }
        sample *= 2
    }
    return sample
}

private fun ceilDiv(
    value: Int,
    divisor: Int,
): Long = (value.toLong() + divisor - 1) / divisor

internal const val IMAGE_EDIT_DECODE_PIXEL_LIMIT = 12_000_000L
