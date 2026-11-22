package com.solthariel.pawchan.media.video

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Straight progress track used by the accessible Slider. */
@Composable
internal fun VideoSeekTrack(
    fraction: Float,
    enabled: Boolean,
    colors: ColorScheme = MaterialTheme.colorScheme,
) {
    val active = videoSeekActiveColor(colors, enabled)
    val inactive = videoSeekInactiveColor(colors, enabled)
    Canvas(Modifier.fillMaxWidth().height(TRACK_HEIGHT)) {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val start = if (rtl) size.width else 0f
        val end = if (rtl) 0f else size.width
        val boundary = videoSeekTrackBoundary(size.width, fraction, rtl)
        val stroke = TRACK_STROKE.toPx()
        drawLine(inactive, Offset(boundary, center.y), Offset(end, center.y), stroke, StrokeCap.Round)
        if (fraction > 0f) {
            drawLine(active, Offset(start, center.y), Offset(boundary, center.y), stroke, StrokeCap.Round)
        }
    }
}

internal fun videoSeekActiveColor(
    colors: ColorScheme,
    enabled: Boolean,
): Color = if (enabled) colors.primary else colors.onSurface.copy(alpha = DISABLED_SEEK_ALPHA)

internal fun videoSeekInactiveColor(
    colors: ColorScheme,
    enabled: Boolean,
): Color =
    if (enabled) {
        colors.onSurfaceVariant.copy(alpha = UNSELECTED_SEEK_ALPHA)
    } else {
        colors.onSurface.copy(alpha = DISABLED_SEEK_ALPHA)
    }

internal fun videoSeekTrackBoundary(
    width: Float,
    fraction: Float,
    rtl: Boolean,
): Float {
    val progress = fraction.coerceIn(0f, 1f)
    return width * if (rtl) 1f - progress else progress
}

private const val UNSELECTED_SEEK_ALPHA = 0.55f
private const val DISABLED_SEEK_ALPHA = 0.38f
private val TRACK_HEIGHT = 12.dp
private val TRACK_STROKE = 3.dp
