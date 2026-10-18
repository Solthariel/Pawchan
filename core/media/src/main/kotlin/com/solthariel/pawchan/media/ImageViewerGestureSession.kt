package com.solthariel.pawchan.media

import kotlin.math.abs
import kotlin.math.hypot

internal enum class ImageGestureMode { WAITING, PAN, TRANSFORM, PAGE, CLOSE }

internal enum class ImageRelease { RESTORE, FLING, PREVIOUS, NEXT, CLOSE }

/** One gesture claims paging, common panning/scaling or vertical close; a claimed close cannot become pinch. */
internal class ImageViewerGestureSession(
    initial: ImageTransform,
    private val flingDistance: Float = 24f,
) {
    var transform: ImageTransform = initial
        private set
    var mode: ImageGestureMode = ImageGestureMode.WAITING
        private set
    var travelX: Float = 0f
        private set
    var travelY: Float = 0f
        private set

    fun move(
        sample: ImageGestureSample,
        viewport: ImageViewport,
        slop: Float,
    ): Boolean {
        if (mode == ImageGestureMode.CLOSE && sample.pointers > 1) return true
        return if (sample.pointers > 1 || mode == ImageGestureMode.TRANSFORM) {
            mode = ImageGestureMode.TRANSFORM
            travelX = 0f
            travelY = 0f
            transform = transform.zoom(sample, viewport)
            true
        } else {
            when (mode) {
                ImageGestureMode.WAITING -> {
                    startMove(sample, viewport, slop)
                }

                ImageGestureMode.PAN -> {
                    pan(sample, viewport)
                    true
                }

                else -> {
                    travelX += sample.dx
                    travelY += sample.dy
                    if (mode == ImageGestureMode.CLOSE &&
                        abs(travelX) >= abs(travelY) * CLOSE_DIRECTION_RATIO &&
                        transform.atHorizontalEdge(travelX, viewport)
                    ) {
                        mode = ImageGestureMode.PAGE
                    }
                    true
                }
            }
        }
    }

    private fun startMove(
        sample: ImageGestureSample,
        viewport: ImageViewport,
        slop: Float,
    ): Boolean {
        travelX += sample.dx
        travelY += sample.dy
        val accepted = hypot(travelX, travelY) >= slop
        if (accepted) {
            mode = chooseMode(viewport)
            if (mode == ImageGestureMode.PAN) pan(sample.copy(dx = travelX, dy = travelY), viewport)
        }
        return accepted
    }

    private fun chooseMode(viewport: ImageViewport): ImageGestureMode {
        val horizontal = abs(travelX) >= abs(travelY) * CLOSE_DIRECTION_RATIO
        return when {
            horizontal && transform.atHorizontalEdge(travelX, viewport) -> ImageGestureMode.PAGE

            abs(travelY) >= abs(travelX) * CLOSE_DIRECTION_RATIO &&
                transform.atVerticalEdge(travelY, viewport) -> ImageGestureMode.CLOSE

            transform.scale > viewport.minimumScale -> ImageGestureMode.PAN

            horizontal -> ImageGestureMode.PAGE

            else -> ImageGestureMode.PAN
        }
    }

    private fun pan(
        sample: ImageGestureSample,
        viewport: ImageViewport,
    ) {
        val proposed = transform.copy(x = transform.x + sample.dx, y = transform.y + sample.dy)
        transform = proposed.bounded(viewport)
        val residual = proposed.x - transform.x
        if (abs(residual) > 1f && abs(sample.dx) >= abs(sample.dy) * CLOSE_DIRECTION_RATIO) {
            mode = ImageGestureMode.PAGE
            travelX = residual
            travelY = 0f
        }
    }

    fun release(
        viewport: ImageViewport,
        velocityX: Float,
        velocityY: Float,
        minVelocity: Float,
    ): ImageRelease =
        when (mode) {
            ImageGestureMode.PAGE -> {
                pageRelease(viewport.width, velocityX, minVelocity)
            }

            ImageGestureMode.CLOSE -> {
                if (abs(travelY) >= abs(travelX) * CLOSE_DIRECTION_RATIO &&
                    closeCommitted(travelY, viewport.height, velocityY)
                ) {
                    ImageRelease.CLOSE
                } else {
                    ImageRelease.RESTORE
                }
            }

            ImageGestureMode.PAN, ImageGestureMode.TRANSFORM -> {
                ImageRelease.FLING
            }

            ImageGestureMode.WAITING -> {
                ImageRelease.RESTORE
            }
        }

    private fun pageRelease(
        width: Float,
        velocity: Float,
        minVelocity: Float,
    ): ImageRelease =
        when {
            abs(travelX) >= flingDistance && abs(velocity) >= minVelocity && travelX * velocity <= 0f -> {
                ImageRelease.RESTORE
            }

            !pageCommitted(travelX, width, velocity, minVelocity, flingDistance) -> {
                ImageRelease.RESTORE
            }

            travelX > 0f -> {
                ImageRelease.PREVIOUS
            }

            else -> {
                ImageRelease.NEXT
            }
        }
}

private fun pageCommitted(
    travel: Float,
    extent: Float,
    velocity: Float,
    minVelocity: Float,
    flingDistance: Float,
): Boolean =
    abs(travel) >= extent * IMAGE_SWIPE_FRACTION ||
        (abs(travel) >= flingDistance && abs(velocity) >= minVelocity && travel * velocity > 0f)

/** Reference dismissal moves at quarter speed and varies its threshold with release velocity. */
private fun closeCommitted(
    travel: Float,
    height: Float,
    velocity: Float,
): Boolean {
    val speed = abs(velocity) * IMAGE_CLOSE_RESISTANCE / CLOSE_VELOCITY_FACTOR
    val thresholdFactor =
        if (travel * velocity >= 0f) {
            1f - speed.coerceAtMost(CLOSE_MAX_VELOCITY_REDUCTION)
        } else {
            speed.coerceAtLeast(1f)
        }
    return abs(travel) >= height * IMAGE_SWIPE_FRACTION * thresholdFactor
}

internal const val IMAGE_CLOSE_RESISTANCE = 0.25f
private const val IMAGE_SWIPE_FRACTION = 0.5f
private const val CLOSE_DIRECTION_RATIO = 1.732f
private const val CLOSE_VELOCITY_FACTOR = 1000f
private const val CLOSE_MAX_VELOCITY_REDUCTION = 0.9f
