package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.solthariel.pawchan.domain.drafts.DraftImageMark
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftShapeKind
import com.solthariel.pawchan.domain.drafts.DraftStroke
import com.solthariel.pawchan.domain.drafts.DraftTextMark

internal fun drawImageEditMark(
    canvas: Canvas,
    output: Bitmap,
    space: ImageEditRasterSpace,
    mark: DraftImageMark,
) {
    when (mark) {
        is DraftStroke -> drawEditStroke(canvas, space, mark)
        is DraftShape -> drawEditShape(canvas, space, mark)
        is DraftTextMark -> drawEditText(canvas, space, mark)
        is DraftRedaction -> drawEditRedaction(canvas, output, space, mark)
    }
}

internal fun drawEditStroke(
    canvas: Canvas,
    space: ImageEditRasterSpace,
    stroke: DraftStroke,
) {
    val paint = editStrokePaint(stroke.color, stroke.width * space.shortSide)
    if (stroke.marker) paint.alpha = (Color.alpha(stroke.color) * MARKER_OPACITY).toInt()
    val points = stroke.points.map(space::map)
    if (points.size == 1) {
        canvas.drawPoint(points[0].x, points[0].y, paint)
    } else {
        canvas.drawPath(editPath(points), paint)
    }
}

internal fun drawEditShape(
    canvas: Canvas,
    space: ImageEditRasterSpace,
    shape: DraftShape,
) {
    val from = space.map(shape.from)
    val to = space.map(shape.to)
    val paint = editStrokePaint(shape.color, shape.width * space.shortSide)
    when (shape.kind) {
        DraftShapeKind.ARROW -> drawEditArrow(canvas, from, to, paint)
        DraftShapeKind.RECTANGLE -> canvas.drawRect(editRect(from, to), paint)
        DraftShapeKind.OVAL -> canvas.drawOval(editRect(from, to), paint)
    }
}

private fun drawEditArrow(
    canvas: Canvas,
    from: ImageEditPoint,
    to: ImageEditPoint,
    paint: Paint,
) {
    canvas.drawLine(from.x, from.y, to.x, to.y, paint)
    val heads = imageEditArrowHeads(from, to, paint.strokeWidth)
    canvas.drawLine(to.x, to.y, heads.first.x, heads.first.y, paint)
    canvas.drawLine(to.x, to.y, heads.second.x, heads.second.y, paint)
}

internal fun editStrokePaint(
    color: Int,
    width: Float,
): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width.coerceAtLeast(1f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

internal fun editPath(points: List<ImageEditPoint>): Path =
    Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }

internal fun editRect(
    a: ImageEditPoint,
    b: ImageEditPoint,
): RectF = RectF(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))

private const val MARKER_OPACITY = 0.45f
