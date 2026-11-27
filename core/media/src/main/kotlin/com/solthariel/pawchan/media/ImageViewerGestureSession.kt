package com.solthariel.pawchan.media

import kotlin.math.abs
import kotlin.math.hypot

internal enum class ImageGestureMode { WAITING, PAN, TRANSFORM, PAGE, CLOSE }

internal enum class ImageRelease { RESTORE, FLING, PREVIOUS, NEXT, CLOSE }

/**
 * One gesture claims paging, common panning/scaling or vertical close; a claimed close cannot become pinch.
 * Paging wins every doubt: a swipe flatter than 45 degrees pages at once, closing needs a near-vertical
 * swipe ([decisionDistance] long, steeper than ~63 degrees), and any real sideways move turns it into paging.
 */
internal class ImageViewerGestureSession(
    initial: ImageTransform,
    private val flingDistance: Float = 24f,
    private val decisionDistance: Float = 25f,
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
                    val sideways = mode == ImageGestureMode.CLOSE && movedSideways()
                    if (sideways && transform.atHorizontalEdge(travelX, viewport)) {
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
        val distance = hypot(travelX, travelY)
        val accepted = distance >= slop
        if (accepted) {
            mode = chooseMode(viewport, distance)
            if (mode == ImageGestureMode.PAN) pan(sample.copy(dx = travelX, dy = travelY), viewport)
        }
        return accepted
    }

    /** WAITING after the touch slop means "not decided yet": the gesture is ours, the direction is not. */
    private fun chooseMode(
        viewport: ImageViewport,
        distance: Float,
    ): ImageGestureMode {
        val flat = abs(travelX) >= abs(travelY)
        val closing = abs(travelY) >= abs(travelX) * CLOSE_STEEPNESS && transform.atVerticalEdge(travelY, viewport)
        val decided = distance >= decisionDistance
        return when {
            flat && transform.atHorizontalEdge(travelX, viewport) -> ImageGestureMode.PAGE
            closing && !decided -> ImageGestureMode.WAITING
            closing -> ImageGestureMode.CLOSE
            transform.scale > viewport.minimumScale -> ImageGestureMode.PAN
            flat || decided -> ImageGestureMode.PAGE
            else -> ImageGestureMode.WAITING
        }
    }

    /** A thumb arc or a sideways correction: no longer the near-vertical motion that closes the viewer. */
    private fun movedSideways(): Boolean {
        val sideways = abs(travelX)
        return sideways >= decisionDistance && abs(travelY) < sideways * CLOSE_STEEPNESS
    }

    private fun pan(
        sample: ImageGestureSample,
        viewport: ImageViewport,
    ) {
        val proposed = transform.copy(x = transform.x + sample.dx, y = transform.y + sample.dy)
        transform = proposed.bounded(viewport)
        val residual = proposed.x - transform.x
        if (abs(residual) > 1f && abs(sample.dx) >= abs(sample.dy)) {
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
                if (abs(travelY) >= abs(travelX) * CLOSE_STEEPNESS &&
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

/**
 * Reference dismissal moves at quarter speed and varies its threshold with release velocity; a fast flick
 * still needs [MIN_CLOSE_FRACTION] of the height, so a short jitter never closes the viewer.
 */
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
    return abs(travel) >= maxOf(height * IMAGE_SWIPE_FRACTION * thresholdFactor, height * MIN_CLOSE_FRACTION)
}

internal const val IMAGE_CLOSE_RESISTANCE = 0.25f
private const val IMAGE_SWIPE_FRACTION = 0.5f

/** tan(63.4 degrees): closing needs a swipe at least twice as long vertically as sideways. */
private const val CLOSE_STEEPNESS = 2f
private const val MIN_CLOSE_FRACTION = 0.12f
private const val CLOSE_VELOCITY_FACTOR = 1000f
private const val CLOSE_MAX_VELOCITY_REDUCTION = 0.9f
