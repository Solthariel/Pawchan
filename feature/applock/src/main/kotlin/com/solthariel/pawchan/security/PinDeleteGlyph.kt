package com.solthariel.pawchan.security

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Drawn rather than a Unicode symbol, so every device gets a real delete icon. */
@Composable
internal fun PinDeleteGlyph(modifier: Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        val unit = size.minDimension / DELETE_GRID
        val outline =
            Path().apply {
                moveTo(unit * DELETE_TIP, unit * DELETE_CENTER)
                lineTo(unit * DELETE_SHOULDER, unit * DELETE_TOP)
                lineTo(unit * DELETE_RIGHT, unit * DELETE_TOP)
                lineTo(unit * DELETE_RIGHT, unit * DELETE_BOTTOM)
                lineTo(unit * DELETE_SHOULDER, unit * DELETE_BOTTOM)
                close()
            }
        drawPath(outline, color, style = Stroke(unit * DELETE_STROKE, cap = StrokeCap.Round))
        drawLine(
            color,
            Offset(unit * DELETE_CROSS_LEFT, unit * DELETE_CROSS_TOP),
            Offset(unit * DELETE_CROSS_RIGHT, unit * DELETE_CROSS_BOTTOM),
            unit * DELETE_STROKE,
            StrokeCap.Round,
        )
        drawLine(
            color,
            Offset(unit * DELETE_CROSS_RIGHT, unit * DELETE_CROSS_TOP),
            Offset(unit * DELETE_CROSS_LEFT, unit * DELETE_CROSS_BOTTOM),
            unit * DELETE_STROKE,
            StrokeCap.Round,
        )
    }
}

private const val DELETE_GRID = 24f
private const val DELETE_TIP = 2f
private const val DELETE_CENTER = 12f
private const val DELETE_SHOULDER = 8f
private const val DELETE_TOP = 5f
private const val DELETE_BOTTOM = 19f
private const val DELETE_RIGHT = 22f
private const val DELETE_STROKE = 1.6f
private const val DELETE_CROSS_LEFT = 12f
private const val DELETE_CROSS_RIGHT = 18f
private const val DELETE_CROSS_TOP = 9f
private const val DELETE_CROSS_BOTTOM = 15f
