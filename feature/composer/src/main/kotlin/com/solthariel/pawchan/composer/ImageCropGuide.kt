package com.solthariel.pawchan.composer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.CanvasCrop

internal fun DrawScope.drawImageCropGuide(crop: CanvasCrop) {
    val left = crop.left * size.width
    val right = crop.right * size.width
    val top = crop.top * size.height
    val bottom = crop.bottom * size.height
    val shade = Color.Black.copy(alpha = 0.55f)
    drawRect(shade, Offset.Zero, Size(size.width, top))
    drawRect(shade, Offset(0f, bottom), Size(size.width, size.height - bottom))
    drawRect(shade, Offset(0f, top), Size(left, bottom - top))
    drawRect(shade, Offset(right, top), Size(size.width - right, bottom - top))
    val stroke = 2.dp.toPx()
    drawRect(Color.White, Offset(left, top), Size(right - left, bottom - top), style = Stroke(stroke))
    for (part in 1..2) {
        val x = left + (right - left) * part / GRID_DIVISOR
        val y = top + (bottom - top) * part / GRID_DIVISOR
        val grid = Color.White.copy(alpha = 0.65f)
        drawLine(grid, Offset(x, top), Offset(x, bottom), 1.dp.toPx())
        drawLine(grid, Offset(left, y), Offset(right, y), 1.dp.toPx())
    }
    val xs = listOf(left, (left + right) / 2f, right)
    val ys = listOf(top, (top + bottom) / 2f, bottom)
    for (x in xs) {
        for (y in ys) {
            if (x != xs[1] || y != ys[1]) drawCircle(Color.White, 4.dp.toPx(), Offset(x, y))
        }
    }
}

private const val GRID_DIVISOR = 3f
