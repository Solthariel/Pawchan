package com.solthariel.pawchan.thread

import androidx.compose.animation.core.Easing
import kotlin.math.abs
import kotlin.math.sqrt

/** Geometry and release speed are normalized to dp and dp/s before reaching this policy. */
internal fun previewSwipeAction(
    distance: Float,
    viewport: Float,
    velocity: Float,
    card: Float = viewport,
): PreviewSwipeAction {
    val height = viewport.coerceAtLeast(1f)
    val relative = (card / height - LARGE_CARD_FRACTION) / (SMALL_CARD_FRACTION - LARGE_CARD_FRACTION)
    val fraction = relative.coerceIn(0f, 1f)
    val denominator = 2f * (fraction + 1f)
    val forwardSpeed = if (distance * velocity > 0f) abs(velocity) else 0f
    val acceleration = 1f + sqrt(forwardSpeed / height)
    return when {
        distance == 0f || acceleration * abs(distance) < height / denominator -> PreviewSwipeAction.RESTORE
        distance < 0f -> PreviewSwipeAction.POP
        else -> PreviewSwipeAction.CLEAR
    }
}

internal fun previewSwipeDuration(
    distance: Float,
    velocity: Float,
    viewport: Float = DEFAULT_VIEWPORT,
): Int {
    val height = viewport.coerceAtLeast(1f)
    val duration =
        if (abs(velocity) > 0f) {
            abs(distance) / abs(velocity) * MILLIS_PER_SECOND * VELOCITY_SETTLE_FACTOR
        } else {
            (abs(distance) / height + 1f) * BASE_SETTLE_DURATION
        }
    return duration.toInt().coerceIn(MIN_SETTLE_DURATION, MAX_SETTLE_DURATION)
}

/** Scroll settling decelerates toward rest instead of running at a fixed linear speed. */
internal val PREVIEW_RESTORE_EASING =
    Easing { fraction ->
        val rest = fraction - 1f
        rest * rest * rest * rest * rest + 1f
    }
private const val MILLIS_PER_SECOND = 1000f
private const val MIN_SETTLE_DURATION = 150
private const val MAX_SETTLE_DURATION = 600

private const val LARGE_CARD_FRACTION = 2f / 3f
private const val SMALL_CARD_FRACTION = 1f / 3f

private const val DEFAULT_VIEWPORT = 1000f
private const val BASE_SETTLE_DURATION = 256
private const val VELOCITY_SETTLE_FACTOR = 4
