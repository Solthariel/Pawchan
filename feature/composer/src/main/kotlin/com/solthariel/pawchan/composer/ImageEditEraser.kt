package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageMark
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftShapeKind
import com.solthariel.pawchan.domain.drafts.DraftStroke
import kotlin.math.cos
import kotlin.math.sin

/** Remove the topmost drawing mark as one history action; text and redactions are not erased. */
internal fun eraseImageDrawing(
    edit: DraftImageEdit,
    space: ImageEditRasterSpace,
    point: ImageEditPoint,
    radius: Float,
): DraftImageEdit {
    require(point.x.isFinite() && point.y.isFinite() && radius.isFinite() && radius >= 0f)
    val index = edit.marks.indexOfLast { mark -> drawingHit(mark, space, point, radius) }
    return if (index < 0) edit else edit.copy(marks = edit.marks.filterIndexed { position, _ -> position != index })
}

private fun drawingHit(
    mark: DraftImageMark,
    space: ImageEditRasterSpace,
    point: ImageEditPoint,
    radius: Float,
): Boolean =
    when (mark) {
        is DraftStroke -> {
            val width = (mark.width * space.shortSide).coerceAtLeast(1f)
            pathHit(point, mark.points.map(space::map), radius + width / 2f)
        }

        is DraftShape -> {
            shapeHit(mark, space, point, radius)
        }

        else -> {
            false
        }
    }

private fun shapeHit(
    shape: DraftShape,
    space: ImageEditRasterSpace,
    point: ImageEditPoint,
    radius: Float,
): Boolean {
    val from = space.map(shape.from)
    val to = space.map(shape.to)
    val width = (shape.width * space.shortSide).coerceAtLeast(1f)
    val tolerance = radius + width / 2f
    return when (shape.kind) {
        DraftShapeKind.ARROW -> {
            val heads = imageEditArrowHeads(from, to, width)
            pathHit(point, listOf(from, to, heads.first), tolerance) ||
                pathHit(point, listOf(to, heads.second), tolerance)
        }

        DraftShapeKind.RECTANGLE -> {
            pathHit(point, rectanglePath(from, to), tolerance)
        }

        DraftShapeKind.OVAL -> {
            pathHit(point, ellipsePath(from, to), tolerance)
        }
    }
}

private fun rectanglePath(
    from: ImageEditPoint,
    to: ImageEditPoint,
): List<ImageEditPoint> {
    val left = minOf(from.x, to.x)
    val right = maxOf(from.x, to.x)
    val top = minOf(from.y, to.y)
    val bottom = maxOf(from.y, to.y)
    return listOf(
        ImageEditPoint(left, top),
        ImageEditPoint(right, top),
        ImageEditPoint(right, bottom),
        ImageEditPoint(left, bottom),
        ImageEditPoint(left, top),
    )
}

private fun ellipsePath(
    from: ImageEditPoint,
    to: ImageEditPoint,
): List<ImageEditPoint> {
    val centerX = (from.x + to.x) / 2f
    val centerY = (from.y + to.y) / 2f
    val rx = kotlin.math.abs(from.x - to.x) / 2f
    val ry = kotlin.math.abs(from.y - to.y) / 2f
    return (0..ELLIPSE_SEGMENTS).map { index ->
        val angle = index * 2.0 * Math.PI / ELLIPSE_SEGMENTS
        ImageEditPoint(centerX + rx * cos(angle).toFloat(), centerY + ry * sin(angle).toFloat())
    }
}

private const val ELLIPSE_SEGMENTS = 64
