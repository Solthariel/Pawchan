package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Палец вверх/вниз в стиле маркеров ОП/sage: контур, заливка — уже оценено. */
@Composable
fun UiRatingIcon(
    up: Boolean,
    filled: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val unit = size.minDimension / RATING_GRID
        val width = minOf(RATING_STROKE.dp.toPx(), size.minDimension * RATING_STROKE_RATIO)
        scale(1f, if (up) 1f else -1f) {
            scale(unit, unit, pivot = Offset.Zero) {
                val gridStyle =
                    if (filled) Fill else Stroke(width / unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawPath(THUMB_PATH, color, style = gridStyle)
                drawPath(CUFF_PATH, color, style = gridStyle)
            }
        }
    }
}

@Composable
@ReadOnlyComposable
fun ratingLikeColor(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE
    return if (dark) Color(LIKE_DARK) else Color(LIKE_LIGHT)
}

@Composable
@ReadOnlyComposable
fun ratingDislikeColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE) Color(DISLIKE_DARK) else Color(DISLIKE_LIGHT)

/** Контуры в сетке 24x24, как у векторных иконок Material. */
private val THUMB_PATH =
    PathParser()
        .parsePathString(
            "M8 10 L11.5 3 Q14.5 2.5 14.2 5.5 L13.6 9 L19.2 9 Q21.6 9.2 21 11.8 L19.6 18.8 Q19.1 21 16.8 21 L8 21 Z",
        ).toPath()
private val CUFF_PATH = PathParser().parsePathString("M2.5 10 L5.5 10 L5.5 21 L2.5 21 Z").toPath()

private const val RATING_GRID = 24f
private const val RATING_STROKE = 2f
private const val RATING_STROKE_RATIO = 0.11f
private const val DARK_SURFACE = 0.4f
private const val LIKE_LIGHT = 0xFF2E7D32
private const val LIKE_DARK = 0xFF66BB6A
private const val DISLIKE_LIGHT = 0xFFC62828
private const val DISLIKE_DARK = 0xFFEF5350
