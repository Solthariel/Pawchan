package com.solthariel.pawchan.media

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal data class ImageViewport(
    val width: Float,
    val height: Float,
    val imageWidth: Float,
    val imageHeight: Float,
) {
    val fittedWidth: Float get() = imageWidth * fit
    val fittedHeight: Float get() = imageHeight * fit
    val minimumScale: Float get() = 1f / max(fit, 1f)
    val maximumScale: Float get() = IMAGE_NATIVE_MAX_SCALE / fit
    val initialScale: Float get() = min(1f, maximumScale)
    val doubleTapScale: Float get() = if (fit > 1f) initialScale else min(1f / fit, IMAGE_DOUBLE_TAP_MAX_SCALE)
    private val fit: Float get() = min(width / imageWidth.coerceAtLeast(1f), height / imageHeight.coerceAtLeast(1f))
}

internal data class ImageTransform(
    val scale: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f,
) {
    fun bounded(viewport: ImageViewport): ImageTransform {
        val horizontal = ((viewport.fittedWidth * scale - viewport.width) / 2f).coerceAtLeast(0f)
        val vertical = ((viewport.fittedHeight * scale - viewport.height) / 2f).coerceAtLeast(0f)
        return copy(x = x.coerceIn(-horizontal, horizontal), y = y.coerceIn(-vertical, vertical))
    }

    fun zoom(
        sample: ImageGestureSample,
        viewport: ImageViewport,
    ): ImageTransform {
        val capped = min(sample.zoom, viewport.maximumScale / scale)
        val softened =
            if (scale <= viewport.minimumScale &&
                capped < 1f
            ) {
                capped.toDouble().pow(IMAGE_UNDERSCALE_POWER).toFloat()
            } else {
                capped
            }
        val factor = max(softened, viewport.minimumScale / scale / 2f)
        val nextScale = scale * factor
        val ratio = nextScale / scale
        val focusX = sample.focusX - viewport.width / 2f
        val focusY = sample.focusY - viewport.height / 2f
        return ImageTransform(
            nextScale,
            (x - focusX) * ratio + focusX + sample.dx,
            (y - focusY) * ratio + focusY + sample.dy,
        ).bounded(viewport)
    }

    fun doubleTap(
        x: Float,
        y: Float,
        viewport: ImageViewport,
    ): ImageTransform =
        if (scale >= viewport.doubleTapScale) {
            copy(scale = viewport.minimumScale, x = 0f, y = 0f)
        } else {
            zoom(ImageGestureSample(0f, 0f, viewport.doubleTapScale / scale, x, y, 2), viewport)
        }

    fun atHorizontalEdge(
        dx: Float,
        viewport: ImageViewport,
    ): Boolean {
        val candidate = copy(x = x + dx).bounded(viewport)
        return abs(candidate.x - x) < IMAGE_EDGE_TOLERANCE
    }

    fun atVerticalEdge(
        dy: Float,
        viewport: ImageViewport,
    ): Boolean {
        val candidate = copy(y = y + dy).bounded(viewport)
        return abs(candidate.y - y) < IMAGE_EDGE_TOLERANCE
    }
}

internal data class ImageGestureSample(
    val dx: Float,
    val dy: Float,
    val zoom: Float,
    val focusX: Float,
    val focusY: Float,
    val pointers: Int,
)

private const val IMAGE_EDGE_TOLERANCE = 0.5f

private const val IMAGE_NATIVE_MAX_SCALE = 8f
private const val IMAGE_DOUBLE_TAP_MAX_SCALE = 8f
private const val IMAGE_UNDERSCALE_POWER = 0.25
