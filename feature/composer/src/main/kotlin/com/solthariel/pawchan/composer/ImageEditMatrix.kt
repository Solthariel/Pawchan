package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit

/** Affine coefficients map crop-local pixels into the rotated/mirrored output. */
internal fun imageEditMatrixValues(
    bounds: ImageEditBounds,
    edit: DraftImageEdit,
): FloatArray {
    val width = (bounds.right - bounds.left).toFloat()
    val height = (bounds.bottom - bounds.top).toFloat()
    val sign = if (edit.mirrored) -1f else 1f
    val offset = if (edit.mirrored) width else 0f
    return when (edit.quarterTurns) {
        0 -> floatArrayOf(sign, 0f, offset, 0f, 1f, 0f, 0f, 0f, 1f)
        1 -> floatArrayOf(0f, -1f, height, sign, 0f, offset, 0f, 0f, 1f)
        2 -> floatArrayOf(-sign, 0f, width - offset, 0f, -1f, height, 0f, 0f, 1f)
        else -> floatArrayOf(0f, 1f, 0f, -sign, 0f, width - offset, 0f, 0f, 1f)
    }
}
