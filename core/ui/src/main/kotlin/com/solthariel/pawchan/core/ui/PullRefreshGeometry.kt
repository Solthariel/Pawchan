package com.solthariel.pawchan.core.ui

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow

internal fun pullRefreshArc(
    strain: Float,
    rotation: Float?,
): Pair<Float, Float> =
    if (rotation == null) {
        val progress = min(strain, 1f).coerceAtLeast(0f)
        (PULL_ARC_ROTATION * strain - PULL_ARC_ROTATION) to
            (MAXIMUM_SPIN_ARC * progress.toDouble().pow(PULL_ARC_POWER).toFloat())
    } else {
        val breathing = ((cos(rotation * 2f * PI) + 1.0) / 2.0).toFloat()
        (rotation * FULL_ROTATION_DEGREES) to (MINIMUM_SPIN_ARC + SPIN_ARC_RANGE * breathing)
    }

internal fun pullRefreshCenterY(
    strain: Float,
    height: Float,
    style: PullRefreshStyle,
    side: PullRefreshSide,
): Float {
    val fromEdge = style.travel * strain - style.radius
    return if (side == PullRefreshSide.TOP) fromEdge else height - style.bottomInset - fromEdge
}

internal data class PullRefreshStyle(
    val radius: Float,
    val travel: Float,
    val ringRadius: Float,
    val strokeWidth: Float,
    val colors: PullRefreshColors,
    val bottomInset: Float,
)

private const val MINIMUM_SPIN_ARC = 30f
private const val SPIN_ARC_RANGE = 240f
private const val PULL_ARC_POWER = 0.75

internal data class PullRefreshColors(
    val background: Color,
    val foreground: Color,
)

private const val PULL_ARC_ROTATION = 90f
private const val MAXIMUM_SPIN_ARC = 270f
private const val FULL_ROTATION_DEGREES = 360f
