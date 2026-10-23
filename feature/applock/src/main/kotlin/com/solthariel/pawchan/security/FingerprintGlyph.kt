package com.solthariel.pawchan.security

import androidx.compose.foundation.Canvas
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

/** Decorative glyph; the parent button owns its accessibility label and real authentication action. */
@Composable
internal fun FingerprintGlyph(modifier: Modifier) {
    val color = LocalContentColor.current
    Canvas(modifier) {
        val scale = size.minDimension
        val stroke = Stroke(width = scale / FINGERPRINT_GRID, cap = StrokeCap.Round)
        for (inset in FINGERPRINT_INSETS) {
            val left = inset / FINGERPRINT_GRID * scale
            val right = scale - left
            val top = left
            val path =
                Path().apply {
                    moveTo(left, scale * FINGERPRINT_BOTTOM)
                    cubicTo(left, top, right, top, right, scale * FINGERPRINT_BOTTOM)
                }
            drawPath(path, color, style = stroke)
        }
        val center =
            Path().apply {
                moveTo(scale / 2, scale / 2)
                cubicTo(
                    scale / 2,
                    scale,
                    scale / FINGERPRINT_TAIL_DIVISOR,
                    scale,
                    scale / FINGERPRINT_TAIL_DIVISOR,
                    scale,
                )
            }
        drawPath(center, color, style = stroke)
    }
}

private const val FINGERPRINT_GRID = 24f
private const val FINGERPRINT_BOTTOM = 0.8f

private const val FINGERPRINT_OUTER = 2f
private const val FINGERPRINT_MIDDLE = 5f
private const val FINGERPRINT_INNER = 8f
private val FINGERPRINT_INSETS = listOf(FINGERPRINT_OUTER, FINGERPRINT_MIDDLE, FINGERPRINT_INNER)
private const val FINGERPRINT_TAIL_DIVISOR = 3f
