package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import androidx.core.graphics.scale
import androidx.core.graphics.withClip
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool

/** Redactions are painted into the output pixels; no editable overlay is shipped with them. */
internal fun drawEditRedaction(
    canvas: Canvas,
    output: Bitmap,
    space: ImageEditRasterSpace,
    redaction: DraftRedaction,
) {
    val path = redactionPath(space, redaction)
    if (redaction.mode == DraftRedactionMode.FILL) {
        canvas.drawPath(path, Paint().apply { color = redaction.opaqueColor })
    } else {
        pixelatePath(canvas, output, path)
    }
}

private fun redactionPath(
    space: ImageEditRasterSpace,
    redaction: DraftRedaction,
): Path {
    val points = redaction.points.map(space::map)
    val width = (space.shortSide * redaction.width).coerceAtLeast(1f)
    return when {
        redaction.tool == DraftRedactionTool.RECTANGLE -> {
            Path().apply { addRect(editRect(points[0], points[1]), Path.Direction.CW) }
        }

        points.size == 1 -> {
            Path().apply { addCircle(points[0].x, points[0].y, width / 2f, Path.Direction.CW) }
        }

        else -> {
            val stroke = editStrokePaint(Color.BLACK, width)
            Path().also { stroke.getFillPath(editPath(points), it) }
        }
    }
}

private fun pixelatePath(
    canvas: Canvas,
    output: Bitmap,
    path: Path,
) {
    val block = (minOf(output.width, output.height) / PIXEL_BLOCK_DIVISOR).coerceAtLeast(MIN_PIXEL_BLOCK)
    val width = (output.width / block).coerceAtLeast(1)
    val height = (output.height / block).coerceAtLeast(1)
    val scaled = output.scale(width, height, filter = false)
    val pixels = if (scaled === output) requireNotNull(output.copy(Bitmap.Config.ARGB_8888, false)) else scaled
    try {
        canvas.withClip(path) {
            // Clear first so transparent mosaic pixels replace, rather than reveal, the original.
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawBitmap(pixels, null, RectF(0f, 0f, output.width.toFloat(), output.height.toFloat()), Paint())
        }
    } finally {
        pixels.recycle()
    }
}

private const val PIXEL_BLOCK_DIVISOR = 60
private const val MIN_PIXEL_BLOCK = 3
