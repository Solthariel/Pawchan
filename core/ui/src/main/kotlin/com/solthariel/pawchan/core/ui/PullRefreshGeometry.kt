package com.solthariel.pawchan.core.ui

import androidx.compose.ui.graphics.Color

internal fun pullRefreshCenterY(
    strain: Float,
    height: Float,
    style: PullRefreshStyle,
    side: PullRefreshSide,
): Float {
    val fromEdge = style.travel * strain.coerceIn(0f, 1f) - style.radius
    return if (side == PullRefreshSide.TOP) fromEdge else height - style.bottomInset - fromEdge
}

internal data class PullRefreshStyle(
    val radius: Float,
    val travel: Float,
    val ringRadius: Float,
    val colors: PullRefreshColors,
    val bottomInset: Float,
)

internal data class PullRefreshColors(
    val background: Color,
    val foreground: Color,
)
