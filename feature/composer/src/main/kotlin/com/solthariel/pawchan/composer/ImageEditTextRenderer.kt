package com.solthariel.pawchan.composer

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.withRotation
import com.solthariel.pawchan.domain.drafts.DraftTextAlignment
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import com.solthariel.pawchan.domain.drafts.DraftTextStyle
import java.util.Locale

internal fun drawEditText(
    canvas: Canvas,
    space: ImageEditRasterSpace,
    mark: DraftTextMark,
) {
    val appearance = mark.appearance
    val anchor = space.map(mark.anchor)
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            textSize = (appearance.size * space.shortSide).coerceAtLeast(MIN_TEXT_SIZE)
            textAlign = Paint.Align.LEFT
        }
    val text = if (appearance.style == DraftTextStyle.MEME) mark.text.uppercase(Locale.ROOT) else mark.text
    val lines = text.split('\n')
    val width = lines.maxOf { paint.measureText(it) }
    val height = (paint.descent() - paint.ascent()) * lines.size
    val left = anchor.x - width / 2f
    val top = anchor.y - height / 2f
    canvas.withRotation(appearance.angle, anchor.x, anchor.y) {
        if (appearance.style == DraftTextStyle.BANNER) {
            val pad = paint.textSize * BANNER_PADDING
            val rect = RectF(left - pad, top - pad, left + width + pad, top + height + pad)
            val background = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = appearance.color }
            canvas.drawRoundRect(rect, pad, pad, background)
        }
        lines.forEachIndexed { index, line ->
            val lineWidth = paint.measureText(line)
            val x = alignedTextStart(left, width, lineWidth, appearance.alignment)
            val y = top - paint.ascent() + index * (paint.descent() - paint.ascent())
            drawEditTextLine(canvas, line, x, y, paint, mark)
        }
    }
}

private fun drawEditTextLine(
    canvas: Canvas,
    line: String,
    x: Float,
    y: Float,
    paint: Paint,
    mark: DraftTextMark,
) {
    val color = mark.appearance.color
    if (mark.appearance.style == DraftTextStyle.MEME) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (paint.textSize * OUTLINE_WIDTH).coerceAtLeast(1f)
        paint.strokeJoin = Paint.Join.ROUND
        paint.color = contrastColor(color)
        canvas.drawText(line, x, y, paint)
    }
    paint.style = Paint.Style.FILL
    paint.color = if (mark.appearance.style == DraftTextStyle.MEME) color else contrastColor(color)
    canvas.drawText(line, x, y, paint)
}

private fun alignedTextStart(
    left: Float,
    width: Float,
    lineWidth: Float,
    alignment: DraftTextAlignment,
): Float =
    when (alignment) {
        DraftTextAlignment.LEFT -> left
        DraftTextAlignment.CENTER -> left + (width - lineWidth) / 2f
        DraftTextAlignment.RIGHT -> left + width - lineWidth
    }

private fun contrastColor(color: Int): Int {
    val brightness = (Color.red(color) * RED_WEIGHT + Color.green(color) * GREEN_WEIGHT + Color.blue(color)) / WEIGHTS
    return if (brightness >= CONTRAST_THRESHOLD) Color.BLACK else Color.WHITE
}

private const val MIN_TEXT_SIZE = 2f
private const val BANNER_PADDING = 0.25f
private const val OUTLINE_WIDTH = 0.08f
private const val RED_WEIGHT = 3
private const val GREEN_WEIGHT = 6
private const val WEIGHTS = 10
private const val CONTRAST_THRESHOLD = 140
