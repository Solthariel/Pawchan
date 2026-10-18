package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun UiStatusIconCanvas(
    kind: UiIconKind,
    color: Color,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val stroke = Stroke(width = STATUS_ICON_STROKE_DP.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            UiIconKind.PIN -> drawPinIcon(color, stroke)
            UiIconKind.LOCK -> drawLockIcon(color, stroke)
            UiIconKind.MEDIA_MULTIPLE -> drawMediaMultipleIcon(color, stroke)
            else -> Unit
        }
    }
}

internal fun DrawScope.drawPinIcon(
    color: Color,
    stroke: Stroke,
) {
    val path =
        Path().apply {
            moveTo(size.width * PIN_LEFT, size.height * PIN_TOP)
            lineTo(size.width * PIN_RIGHT, size.height * PIN_TOP)
            lineTo(size.width * PIN_INNER_RIGHT, size.height * PIN_SHOULDER)
            lineTo(size.width * PIN_EDGE_RIGHT, size.height * PIN_BASE)
            lineTo(size.width * PIN_EDGE_LEFT, size.height * PIN_BASE)
            lineTo(size.width * PIN_INNER_LEFT, size.height * PIN_SHOULDER)
            close()
        }
    drawPath(path, color, style = stroke)
    drawLine(
        color = color,
        start = Offset(size.width * CENTER, size.height * PIN_BASE),
        end = Offset(size.width * CENTER, size.height * PIN_POINT),
        strokeWidth = stroke.width,
        cap = StrokeCap.Round,
    )
}

internal fun DrawScope.drawLockIcon(
    color: Color,
    stroke: Stroke,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * LOCK_LEFT, size.height * LOCK_BODY_TOP),
        size = Size(size.width * LOCK_WIDTH, size.height * LOCK_BODY_HEIGHT),
        cornerRadius = CornerRadius(stroke.width * LOCK_CORNER_MULTIPLIER),
        style = stroke,
    )
    drawPath(lockShackle(), color, style = stroke)
}

private fun DrawScope.lockShackle(): Path =
    Path().apply {
        moveTo(size.width * LOCK_SHACKLE_LEFT, size.height * LOCK_BODY_TOP)
        lineTo(size.width * LOCK_SHACKLE_LEFT, size.height * LOCK_SHACKLE_BOTTOM)
        cubicTo(
            size.width * LOCK_SHACKLE_LEFT,
            size.height * LOCK_SHACKLE_TOP,
            size.width * LOCK_SHACKLE_RIGHT,
            size.height * LOCK_SHACKLE_TOP,
            size.width * LOCK_SHACKLE_RIGHT,
            size.height * LOCK_SHACKLE_BOTTOM,
        )
        lineTo(size.width * LOCK_SHACKLE_RIGHT, size.height * LOCK_BODY_TOP)
    }

internal fun DrawScope.drawMediaMultipleIcon(
    color: Color,
    stroke: Stroke,
) {
    val shortEdge = size.minDimension * MEDIA_EDGE
    val inset = size.minDimension * MEDIA_INSET
    mediaCorners(inset, shortEdge).forEach { (start, end) ->
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = stroke.width,
            cap = StrokeCap.Square,
        )
    }
}

private fun DrawScope.mediaCorners(
    inset: Float,
    edge: Float,
): List<Pair<Offset, Offset>> =
    listOf(
        Offset(inset, inset) to Offset(inset + edge, inset),
        Offset(inset, inset) to Offset(inset, inset + edge),
        Offset(size.width - inset, inset) to Offset(size.width - inset - edge, inset),
        Offset(size.width - inset, inset) to Offset(size.width - inset, inset + edge),
        Offset(inset, size.height - inset) to Offset(inset + edge, size.height - inset),
        Offset(inset, size.height - inset) to Offset(inset, size.height - inset - edge),
        Offset(size.width - inset, size.height - inset) to
            Offset(size.width - inset - edge, size.height - inset),
        Offset(size.width - inset, size.height - inset) to
            Offset(size.width - inset, size.height - inset - edge),
    )

private const val CENTER = 0.5f
private const val PIN_LEFT = 0.32f
private const val PIN_RIGHT = 0.68f
private const val PIN_TOP = 0.16f
private const val PIN_INNER_RIGHT = 0.62f
private const val PIN_INNER_LEFT = 0.38f
private const val PIN_SHOULDER = 0.42f
private const val PIN_EDGE_RIGHT = 0.76f
private const val PIN_EDGE_LEFT = 0.24f
private const val PIN_BASE = 0.58f
private const val PIN_POINT = 0.9f
private const val LOCK_LEFT = 0.2f
private const val LOCK_BODY_TOP = 0.45f
private const val LOCK_WIDTH = 0.6f
private const val LOCK_BODY_HEIGHT = 0.42f
private const val LOCK_CORNER_MULTIPLIER = 1.4f
private const val LOCK_SHACKLE_LEFT = 0.34f
private const val LOCK_SHACKLE_RIGHT = 0.66f
private const val LOCK_SHACKLE_BOTTOM = 0.34f
private const val LOCK_SHACKLE_TOP = 0.12f
private const val MEDIA_EDGE = 0.25f
private const val MEDIA_INSET = 0.12f
private const val STATUS_ICON_STROKE_DP = 1.35f
