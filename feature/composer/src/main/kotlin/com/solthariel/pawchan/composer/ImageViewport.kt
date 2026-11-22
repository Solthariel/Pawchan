package com.solthariel.pawchan.composer

import kotlin.math.abs

/** View-only state: zoom never changes the edit recipe or exported pixels. */
internal data class ImageViewport(
    val scale: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
) {
    fun transform(motion: ImageViewportMotion): ImageViewport {
        if (!motion.valid) return this
        val nextScale = (scale * motion.zoom).coerceIn(1f, MAX_VIEWPORT_SCALE)
        return if (nextScale == 1f) {
            ImageViewport()
        } else {
            val factor = nextScale / scale
            val nextX = (x - motion.focusX) * factor + motion.focusX + motion.panX
            val nextY = (y - motion.focusY) * factor + motion.focusY + motion.panY
            val limitX = motion.width * (nextScale - 1f) / 2f
            val limitY = motion.height * (nextScale - 1f) / 2f
            ImageViewport(nextScale, nextX.coerceIn(-limitX, limitX), nextY.coerceIn(-limitY, limitY))
        }
    }
}

internal data class ImageViewportMotion(
    val zoom: Float,
    val panX: Float,
    val panY: Float,
    val focusX: Float,
    val focusY: Float,
    val width: Float,
    val height: Float,
) {
    val valid: Boolean get() =
        listOf(zoom, panX, panY, focusX, focusY, width, height).all { it.isFinite() && abs(it) <= MAX_MOTION_VALUE } &&
            zoom > 0f && width > 0f && height > 0f
}

private const val MAX_VIEWPORT_SCALE = 8f
private const val MAX_MOTION_VALUE = 1_000_000f
