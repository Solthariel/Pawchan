package com.solthariel.pawchan.domain.drafts

internal fun freeCropResize(
    bounds: CanvasCrop,
    handle: DraftCropHandle,
    point: ImageCanvasPoint,
    size: CropCanvasSize,
): CanvasCrop? {
    val minX = 1f / size.width
    val minY = 1f / size.height
    val impossible =
        (handle.horizontal < 0 && bounds.right < minX) ||
            (handle.horizontal > 0 && bounds.left > 1f - minX) ||
            (handle.vertical < 0 && bounds.bottom < minY) ||
            (handle.vertical > 0 && bounds.top > 1f - minY)
    if (impossible) return null
    return CanvasCrop(
        if (handle.horizontal < 0) point.x.coerceIn(0f, bounds.right - minX) else bounds.left,
        if (handle.vertical < 0) point.y.coerceIn(0f, bounds.bottom - minY) else bounds.top,
        if (handle.horizontal > 0) point.x.coerceIn(bounds.left + minX, 1f) else bounds.right,
        if (handle.vertical > 0) point.y.coerceIn(bounds.top + minY, 1f) else bounds.bottom,
    )
}

internal fun aspectCropResize(
    bounds: CanvasCrop,
    handle: DraftCropHandle,
    point: ImageCanvasPoint,
    size: CropCanvasSize,
    aspect: Float,
): CanvasCrop? {
    val h = handle.horizontal
    val v = handle.vertical
    val anchorX =
        if (h < 0) {
            bounds.right
        } else if (h > 0) {
            bounds.left
        } else {
            (bounds.left + bounds.right) / 2f
        }
    val anchorY =
        if (v < 0) {
            bounds.bottom
        } else if (v > 0) {
            bounds.top
        } else {
            (bounds.top + bounds.bottom) / 2f
        }
    val maxWidth = axisRoom(anchorX, h) * size.width
    val maxHeight = axisRoom(anchorY, v) * size.height
    val minHeight = maxOf(1f, 1f / aspect)
    val limitHeight = minOf(maxHeight, maxWidth / aspect)
    if (limitHeight < minHeight) return null
    val requestedWidth = if (h == 0) 0f else (point.x - anchorX) * h * size.width
    val requestedHeight = if (v == 0) 0f else (point.y - anchorY) * v * size.height
    val desiredHeight = requestedHeightForHandle(h, v, requestedWidth, requestedHeight, aspect)
    val height = desiredHeight.coerceIn(minHeight, limitHeight) / size.height
    val width = height * size.height * aspect / size.width
    return cropFromAnchor(anchorX, anchorY, width, height, handle)
}

private fun axisRoom(
    anchor: Float,
    direction: Int,
): Float =
    if (direction < 0) {
        anchor
    } else if (direction > 0) {
        1f - anchor
    } else {
        2f * minOf(anchor, 1f - anchor)
    }

private fun requestedHeightForHandle(
    horizontal: Int,
    vertical: Int,
    width: Float,
    height: Float,
    aspect: Float,
): Float =
    when {
        horizontal == 0 -> height
        vertical == 0 -> width / aspect
        else -> (aspect * width + height) / (aspect * aspect + 1f)
    }

private fun cropFromAnchor(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    handle: DraftCropHandle,
): CanvasCrop {
    val left =
        if (handle.horizontal < 0) {
            x - width
        } else if (handle.horizontal > 0) {
            x
        } else {
            x - width / 2f
        }
    val top =
        if (handle.vertical < 0) {
            y - height
        } else if (handle.vertical > 0) {
            y
        } else {
            y - height / 2f
        }
    return CanvasCrop(
        left.coerceIn(0f, 1f),
        top.coerceIn(0f, 1f),
        (left + width).coerceIn(0f, 1f),
        (top + height).coerceIn(0f, 1f),
    )
}
