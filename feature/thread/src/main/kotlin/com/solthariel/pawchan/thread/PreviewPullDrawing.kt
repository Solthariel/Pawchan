package com.solthariel.pawchan.thread

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal fun previewLayerShift(
    offset: Float,
    depth: Int,
    count: Int,
): Float =
    when {
        depth == 0 -> offset
        offset <= 0f -> 0f
        else -> offset * (count - depth).coerceAtLeast(0).toFloat() / count.coerceAtLeast(1)
    }

/** Direction feedback grows continuously; it is not a second dismiss threshold. */
internal fun DrawScope.drawPreviewPullHint(
    offset: Float,
    viewport: Float,
    tint: Color,
) {
    val progress = (abs(offset) / (viewport / HINT_PROGRESS_FACTOR).coerceAtLeast(1f)).coerceIn(0f, 1f)
    if (progress == 0f) return
    val x = size.width / 2f
    val y = if (offset > 0f) 12.dp.toPx() else size.height - 12.dp.toPx()
    val direction = if (offset > 0f) 1f else -1f
    val half = 7.dp.toPx() * progress
    val tip = Offset(x, y + direction * half)
    val color = tint.copy(alpha = tint.alpha * progress)
    drawLine(color, Offset(x - half, y), tip, 2.dp.toPx(), StrokeCap.Round)
    drawLine(color, tip, Offset(x + half, y), 2.dp.toPx(), StrokeCap.Round)
}

private const val HINT_PROGRESS_FACTOR = 8f
