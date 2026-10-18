package com.solthariel.pawchan.core.ui

import kotlin.math.floor
import kotlin.math.roundToInt

/** Uses known item sizes when supplied; otherwise projects from average row height. */
internal data class FastScrollGeometry(
    val totalItems: Int,
    val viewportHeight: Int,
    val averageItemHeight: Float,
    val itemSpacing: Int,
    val contentPadding: FastScrollContentPadding,
    val itemSizes: FastScrollItemSizes? = null,
) {
    val beforePadding: Int
        get() = contentPadding.before
    val afterPadding: Int
        get() = contentPadding.after

    private val stride = (averageItemHeight.toDouble() + itemSpacing).coerceAtLeast(1.0)
    private val contentHeight = itemSizes?.totalHeight?.toDouble() ?: (totalItems * stride - itemSpacing)
    private val scrollRange =
        (contentHeight + beforePadding + afterPadding - viewportHeight).coerceAtLeast(0.0)

    fun progress(
        firstIndex: Int,
        firstOffset: Int,
        canScrollBackward: Boolean,
        canScrollForward: Boolean,
    ): Float =
        when {
            !canScrollBackward -> 0f
            !canScrollForward -> 1f
            scrollRange <= 0.0 -> UNKNOWN_PROGRESS
            else -> (distanceFor(firstIndex, firstOffset) / scrollRange).toFloat().coerceIn(MIN_PROGRESS, MAX_PROGRESS)
        }

    fun target(progress: Float): FastScrollTarget {
        if (totalItems <= 0) return FastScrollTarget(0, 0)
        val distance = scrollRange * progress.coerceIn(0f, 1f)
        return itemSizes?.target(distance) ?: estimatedTarget(distance)
    }

    private fun estimatedTarget(distance: Double): FastScrollTarget {
        val index = floor(distance / stride).toInt().coerceIn(0, totalItems - 1)
        val offset = (distance - index * stride).roundToInt().coerceAtLeast(0)
        return FastScrollTarget(index, offset)
    }

    private fun distanceFor(
        index: Int,
        offset: Int,
    ): Double = (itemSizes?.offsetFor(index)?.toDouble() ?: (index * stride)) + offset
}

internal data class FastScrollContentPadding(
    val before: Int,
    val after: Int,
)

internal data class FastScrollTarget(
    val index: Int,
    val offset: Int,
)

private const val UNKNOWN_PROGRESS = 0.5f
private const val MIN_PROGRESS = 0.0001f
private const val MAX_PROGRESS = 0.9999f
