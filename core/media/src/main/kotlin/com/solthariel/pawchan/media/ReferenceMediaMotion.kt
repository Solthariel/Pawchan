package com.solthariel.pawchan.media

import androidx.compose.animation.core.Easing
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

/** Reference formulas, in viewport-relative coordinates; no Android drawing or storage dependencies. */
internal object ReferenceMediaMotion {
    val zoomEasing = Easing { ((1.0 - cos(Math.PI * it)) / 2.0).toFloat() }
    val restoreEasing = Easing { 1f - (1f - it) * (1f - it) }
    val pagingEasing =
        Easing {
            val left = it - 1f
            left * left * left * left * left + 1f
        }

    fun imageAlpha(
        shift: Float,
        height: Float,
    ): Float = 1f - (abs(shift) * 2f / height).coerceIn(0f, 1f)

    fun imageScale(
        shift: Float,
        height: Float,
    ): Float {
        val progress = (abs(shift) * 2f / height).coerceIn(0f, 1f)
        return 1f - progress * progress * CLOSE_SCALE_REDUCTION
    }

    fun backgroundAlpha(
        shift: Float,
        height: Float,
    ): Float = if (shift >= 0f) 1f else 1f - (abs(shift) * BACKDROP_FADE_MULTIPLIER / height).coerceIn(0f, 1f)

    fun closeDuration(
        distance: Float,
        velocity: Float,
    ): Int =
        (abs(distance / (abs(velocity) * IMAGE_CLOSE_RESISTANCE)) * MILLIS_PER_SECOND)
            .toInt()
            .coerceIn(RESTORE_MS, CLOSE_MAX_MS)

    fun pageDuration(
        distance: Float,
        width: Float,
        velocity: Float,
        minimumVelocity: Float,
    ): Int {
        val duration =
            if (abs(velocity) > minimumVelocity) {
                PAGE_DURATION_MULTIPLIER * (MILLIS_PER_SECOND * abs(distance / velocity)).toInt()
            } else {
                ((abs(distance) / width + 1f) * ZOOM_MS).toInt()
            }
        return min(duration, PAGE_MAX_MS)
    }

    const val ZOOM_MS = 200
    const val RESTORE_MS = 150
    const val PAGE_GAP_DP = 16f
    private const val CLOSE_MAX_MS = 500
    private const val PAGE_MAX_MS = 600
    private const val CLOSE_SCALE_REDUCTION = 0.4f
}

private const val MILLIS_PER_SECOND = 1000f
private const val PAGE_DURATION_MULTIPLIER = 4

private const val BACKDROP_FADE_MULTIPLIER = 4f
