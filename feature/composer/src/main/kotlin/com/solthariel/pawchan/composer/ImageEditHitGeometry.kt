package com.solthariel.pawchan.composer

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

internal fun pathHit(
    point: ImageEditPoint,
    path: List<ImageEditPoint>,
    tolerance: Float,
): Boolean {
    if (path.size == 1) return pointDistanceSquared(point, path.single()) <= tolerance * tolerance
    return path.zipWithNext().any { (a, b) -> segmentDistanceSquared(point, a, b) <= tolerance * tolerance }
}

private fun segmentDistanceSquared(
    point: ImageEditPoint,
    a: ImageEditPoint,
    b: ImageEditPoint,
): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val length = dx * dx + dy * dy
    if (length == 0f) return pointDistanceSquared(point, a)
    val along = (((point.x - a.x) * dx + (point.y - a.y) * dy) / length).coerceIn(0f, 1f)
    return pointDistanceSquared(point, ImageEditPoint(a.x + along * dx, a.y + along * dy))
}

private fun pointDistanceSquared(
    a: ImageEditPoint,
    b: ImageEditPoint,
): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return dx * dx + dy * dy
}

internal fun imageEditArrowHeads(
    from: ImageEditPoint,
    to: ImageEditPoint,
    width: Float,
): Pair<ImageEditPoint, ImageEditPoint> {
    val angle = atan2(to.y - from.y, to.x - from.x)
    val length = maxOf(width * ARROW_HEAD_WIDTHS, MIN_ARROW_HEAD)

    fun endpoint(direction: Float) = ImageEditPoint(to.x - length * cos(direction), to.y - length * sin(direction))
    return endpoint(angle + ARROW_ANGLE) to endpoint(angle - ARROW_ANGLE)
}

private const val ARROW_HEAD_WIDTHS = 4f
private const val MIN_ARROW_HEAD = 8f
private const val ARROW_ANGLE = 0.52f
