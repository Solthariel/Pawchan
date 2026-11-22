package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.CanvasCrop
import com.solthariel.pawchan.domain.drafts.DraftCropHandle
import kotlin.math.abs

/** Pixel coordinates allow a generous touch target regardless of the displayed image's aspect. */
internal fun cropHandleAt(
    crop: CanvasCrop,
    width: Float,
    height: Float,
    x: Float,
    y: Float,
    radius: Float,
): DraftCropHandle? {
    if (width <= 0f || height <= 0f || radius < 0f) return null
    val left = crop.left * width
    val right = crop.right * width
    val top = crop.top * height
    val bottom = crop.bottom * height
    val horizontal = closestEdge(x, left, right, radius)
    val vertical = closestEdge(y, top, bottom, radius)
    val onHorizontalEdge = horizontal != 0 && y in (top - radius)..(bottom + radius)
    val onVerticalEdge = vertical != 0 && x in (left - radius)..(right + radius)
    val h = if (onHorizontalEdge) horizontal else 0
    val v = if (onVerticalEdge) vertical else 0
    return DraftCropHandle.entries.firstOrNull { it.horizontal == h && it.vertical == v }
}

private fun closestEdge(
    coordinate: Float,
    near: Float,
    far: Float,
    radius: Float,
): Int {
    val nearDistance = abs(coordinate - near)
    val farDistance = abs(coordinate - far)
    return when {
        nearDistance <= radius && nearDistance <= farDistance -> -1
        farDistance <= radius -> 1
        else -> 0
    }
}

internal fun cropHandlePosition(
    crop: CanvasCrop,
    handle: DraftCropHandle,
): com.solthariel.pawchan.domain.drafts.ImageCanvasPoint =
    com.solthariel.pawchan.domain.drafts.ImageCanvasPoint(
        if (handle.horizontal < 0) {
            crop.left
        } else if (handle.horizontal > 0) {
            crop.right
        } else {
            (crop.left + crop.right) / 2f
        },
        if (handle.vertical < 0) {
            crop.top
        } else if (handle.vertical > 0) {
            crop.bottom
        } else {
            (crop.top + crop.bottom) / 2f
        },
    )
