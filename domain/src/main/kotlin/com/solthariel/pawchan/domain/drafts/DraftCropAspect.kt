package com.solthariel.pawchan.domain.drafts

/** Center-crops the current crop to an output aspect ratio without enlarging it. */
fun DraftImageEdit.cropToAspect(
    sourceWidth: Int,
    sourceHeight: Int,
    outputAspect: Float,
): DraftImageEdit {
    require(sourceWidth > 0 && sourceHeight > 0)
    require(outputAspect.isFinite() && outputAspect in MIN_ASPECT..MAX_ASPECT)
    val sourceAspect = if (quarterTurns % 2 == 0) outputAspect.toDouble() else 1.0 / outputAspect
    val cropWidth = (crop.right - crop.left).toDouble() * sourceWidth
    val cropHeight = (crop.bottom - crop.top).toDouble() * sourceHeight
    val fittedWidth = minOf(cropWidth, cropHeight * sourceAspect)
    val fittedHeight = minOf(cropHeight, cropWidth / sourceAspect)
    val halfWidth = (fittedWidth / sourceWidth / 2.0).toFloat()
    val halfHeight = (fittedHeight / sourceHeight / 2.0).toFloat()
    val centerX = (crop.left + crop.right) / 2f
    val centerY = (crop.top + crop.bottom) / 2f
    return copy(
        crop =
            DraftCrop(
                left = (centerX - halfWidth).coerceIn(crop.left, crop.right),
                top = (centerY - halfHeight).coerceIn(crop.top, crop.bottom),
                right = (centerX + halfWidth).coerceIn(crop.left, crop.right),
                bottom = (centerY + halfHeight).coerceIn(crop.top, crop.bottom),
            ),
    )
}

/** The original, EXIF-oriented image aspect in the current output orientation. */
fun DraftImageEdit.originalOutputAspect(
    sourceWidth: Int,
    sourceHeight: Int,
): Float {
    require(sourceWidth > 0 && sourceHeight > 0)
    return if (quarterTurns % 2 == 0) {
        sourceWidth.toFloat() / sourceHeight
    } else {
        sourceHeight.toFloat() / sourceWidth
    }
}

private const val MIN_ASPECT = 0.001f
private const val MAX_ASPECT = 1000f
