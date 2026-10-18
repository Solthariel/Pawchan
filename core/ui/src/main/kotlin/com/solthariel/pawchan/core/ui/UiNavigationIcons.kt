package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun UiNavigationIcon(
    kind: UiIconKind,
    color: Color,
    modifier: Modifier,
) {
    Canvas(modifier) {
        val stroke = Stroke(width = NAVIGATION_STROKE_DP.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            UiIconKind.CLOSE -> drawCloseIcon(color, stroke)
            UiIconKind.SETTINGS -> drawSettingsIcon(color, stroke)
            else -> Unit
        }
    }
}

private fun DrawScope.drawCloseIcon(
    color: Color,
    stroke: Stroke,
) {
    drawLine(
        color,
        Offset(size.width * CLOSE_START, size.height * CLOSE_START),
        Offset(size.width * CLOSE_END, size.height * CLOSE_END),
        stroke.width,
        StrokeCap.Round,
    )
    drawLine(
        color,
        Offset(size.width * CLOSE_END, size.height * CLOSE_START),
        Offset(size.width * CLOSE_START, size.height * CLOSE_END),
        stroke.width,
        StrokeCap.Round,
    )
}

private fun DrawScope.drawSettingsIcon(
    color: Color,
    stroke: Stroke,
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(color, size.minDimension * SETTINGS_OUTER_RADIUS, center, style = stroke)
    drawCircle(color, size.minDimension * SETTINGS_INNER_RADIUS, center, style = stroke)
    SETTINGS_ANGLES.forEach { angle ->
        val radians = Math.toRadians(angle.toDouble())
        val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
        drawLine(
            color = color,
            start = center + direction * size.minDimension * SETTINGS_OUTER_RADIUS,
            end = center + direction * size.minDimension * SETTINGS_TOOTH_RADIUS,
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

private val SETTINGS_ANGLES =
    listOf(
        SETTINGS_ANGLE_RIGHT,
        SETTINGS_ANGLE_BOTTOM,
        SETTINGS_ANGLE_LEFT,
        SETTINGS_ANGLE_TOP,
    )
private const val CLOSE_START = 0.28f
private const val CLOSE_END = 0.72f
private const val SETTINGS_INNER_RADIUS = 0.12f
private const val SETTINGS_OUTER_RADIUS = 0.28f
private const val SETTINGS_TOOTH_RADIUS = 0.42f
private const val NAVIGATION_STROKE_DP = 2f
private const val SETTINGS_ANGLE_RIGHT = 0f
private const val SETTINGS_ANGLE_BOTTOM = 90f
private const val SETTINGS_ANGLE_LEFT = 180f
private const val SETTINGS_ANGLE_TOP = 270f
