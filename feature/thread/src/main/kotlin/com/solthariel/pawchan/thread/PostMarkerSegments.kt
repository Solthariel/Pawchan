package com.solthariel.pawchan.thread

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Equal visible segments; the first and last meet the solid marker's exact bounds. */
internal data class PostMarkerSegments(
    val count: Int,
    val length: Float,
    val gap: Float,
)

internal fun postMarkerSegments(
    height: Float,
    density: Float,
): PostMarkerSegments {
    if (height <= 0f) return PostMarkerSegments(0, 0f, 0f)
    val gap = (MARKER_GAP_DP * density).coerceAtMost(height / MIN_SEGMENT_PARTS)
    val target = MARKER_SEGMENT_DP * density
    val count = ((height + gap) / (target + gap)).roundToInt().coerceAtLeast(2)
    return PostMarkerSegments(count, (height - gap * (count - 1)) / count, gap)
}

internal fun Modifier.postCardMarker(
    changes: ThreadPostChanges,
    ownColor: Color,
    replyColor: Color,
): Modifier {
    if (!changes.own && !changes.replyToMe) return this
    val color = if (changes.own) ownColor else replyColor
    return drawWithCache {
        val top = MARKER_INSET.toPx()
        val height = (size.height - top * 2).coerceAtLeast(0f)
        val width = MARKER_WIDTH.toPx()
        val corners = CornerRadius(width / 2)
        val segments = postMarkerSegments(height, density)
        onDrawBehind {
            if (height > 0f && changes.own) {
                drawRoundRect(color, Offset(0f, top), Size(width, height), corners)
            } else {
                repeat(segments.count) { index ->
                    val start = index * (segments.length + segments.gap)
                    // Anchor the last edge explicitly to avoid floating-point overshoot.
                    val end = if (index == segments.count - 1) height else start + segments.length
                    drawRoundRect(color, Offset(0f, top + start), Size(width, end - start), corners)
                }
            }
        }
    }
}

private val MARKER_WIDTH = 3.dp
private val MARKER_INSET = 10.dp
private const val MARKER_SEGMENT_DP = 24f
private const val MARKER_GAP_DP = 4f
private const val MIN_SEGMENT_PARTS = 3f
