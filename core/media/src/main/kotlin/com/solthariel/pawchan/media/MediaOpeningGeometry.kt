package com.solthariel.pawchan.media

import androidx.compose.ui.geometry.Rect

/** Rectangles use screen pixels; the result is local to the viewer window. */
internal fun openingFrame(
    origin: Rect,
    viewer: Rect,
    imageWidth: Float,
    imageHeight: Float,
    progress: Float,
): Rect? {
    if (!validOpening(origin, viewer, imageWidth, imageHeight)) return null
    val viewport = ImageViewport(viewer.width, viewer.height, imageWidth, imageHeight)
    val width = viewport.fittedWidth * viewport.initialScale
    val height = viewport.fittedHeight * viewport.initialScale
    val x = (viewer.width - width) / 2f
    val y = (viewer.height - height) / 2f
    val start =
        Rect(
            origin.left - viewer.left,
            origin.top - viewer.top,
            origin.right - viewer.left,
            origin.bottom - viewer.top,
        )
    val target = Rect(x, y, x + width, y + height)
    val t = progress.coerceIn(0f, 1f)
    return Rect(
        start.left + (target.left - start.left) * t,
        start.top + (target.top - start.top) * t,
        start.right + (target.right - start.right) * t,
        start.bottom + (target.bottom - start.bottom) * t,
    )
}

private fun validOpening(
    origin: Rect,
    viewer: Rect,
    imageWidth: Float,
    imageHeight: Float,
): Boolean = validRect(origin) && validViewer(viewer, imageWidth, imageHeight) && origin.overlaps(viewer)

private fun validViewer(
    viewer: Rect,
    imageWidth: Float,
    imageHeight: Float,
): Boolean = validRect(viewer) && imageWidth > 0f && imageHeight > 0f

private fun validRect(rect: Rect): Boolean = rect.width > 0f && rect.height > 0f
