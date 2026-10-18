package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun UiPostingIcon(
    kind: UiIconKind,
    color: Color,
    modifier: Modifier,
) {
    Canvas(modifier) {
        // Толщина линии пропорциональна размеру: 24dp-кнопки сохраняют 2dp, маленький маркер не «жирнеет».
        val width = minOf(POSTING_ICON_STROKE.dp.toPx(), size.minDimension * POSTING_ICON_STROKE_RATIO)
        val stroke = Stroke(width, cap = StrokeCap.Round)
        if (kind == UiIconKind.SAGE) drawSageIcon(color, stroke) else drawThreadAuthorIcon(color, stroke.width)
    }
}

private fun DrawScope.drawSageIcon(
    color: Color,
    stroke: Stroke,
) {
    val center = size.width * ICON_CENTER
    drawLine(
        color,
        Offset(center, size.height * ICON_TOP),
        Offset(center, size.height * ICON_BOTTOM),
        stroke.width,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(size.width * ARROW_LEFT, size.height * ARROW_TOP),
        Offset(center, size.height * ICON_BOTTOM),
        stroke.width,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(size.width * ARROW_RIGHT, size.height * ARROW_TOP),
        Offset(center, size.height * ICON_BOTTOM),
        stroke.width,
        StrokeCap.Round,
    )
}

/** Shared author marker for standalone icons and text-layout decorations. */
fun DrawScope.drawThreadAuthorIcon(
    color: Color,
    strokeWidth: Float,
) {
    val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
    drawCircle(
        color,
        size.width * HEAD_RADIUS,
        Offset(size.width * ICON_CENTER, size.height * HEAD_CENTER),
        style = stroke,
    )
    drawRoundRect(
        color,
        Offset(size.width * BODY_LEFT, size.height * BODY_TOP),
        Size(size.width * BODY_WIDTH, size.height * BODY_HEIGHT),
        CornerRadius(size.width * BODY_RADIUS),
        style = stroke,
    )
}

private const val POSTING_ICON_STROKE = 2f
private const val POSTING_ICON_STROKE_RATIO = 0.11f
private const val ICON_CENTER = 0.5f
private const val ICON_TOP = 0.16f
private const val ICON_BOTTOM = 0.84f
private const val ARROW_LEFT = 0.22f
private const val ARROW_RIGHT = 0.78f
private const val ARROW_TOP = 0.55f
private const val HEAD_RADIUS = 0.16f
private const val HEAD_CENTER = 0.28f
private const val BODY_LEFT = 0.2f
private const val BODY_TOP = 0.56f
private const val BODY_WIDTH = 0.6f
private const val BODY_HEIGHT = 0.29f
private const val BODY_RADIUS = 0.1f
