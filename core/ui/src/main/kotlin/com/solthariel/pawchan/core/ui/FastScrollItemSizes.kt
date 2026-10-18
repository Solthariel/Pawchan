package com.solthariel.pawchan.core.ui

import kotlin.math.roundToLong

/** Immutable pixel prefix sums for a list whose item heights are known before layout. */
class FastScrollItemSizes(
    heights: IntArray,
) {
    private val offsets = LongArray(heights.size + 1)
    val itemCount: Int = heights.size
    val totalHeight: Long
        get() = offsets.last()

    init {
        for (index in heights.indices) {
            require(heights[index] > 0) { "Item height must be positive" }
            offsets[index + 1] = offsets[index] + heights[index]
        }
    }

    internal fun offsetFor(index: Int): Long = offsets[index.coerceIn(0, itemCount)]

    fun heightAt(index: Int): Int = (offsets[index + 1] - offsets[index]).toInt()

    internal fun target(distance: Double): FastScrollTarget {
        if (itemCount == 0) return FastScrollTarget(0, 0)
        val pixel = distance.roundToLong().coerceIn(0L, totalHeight - 1L)
        var low = 0
        var high = itemCount - 1
        while (low < high) {
            val middle = low + (high - low + 1) / 2
            if (offsets[middle] <= pixel) low = middle else high = middle - 1
        }
        return FastScrollTarget(low, (pixel - offsets[low]).toInt())
    }
}
