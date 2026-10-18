package com.solthariel.pawchan.core.ui

import com.solthariel.pawchan.core.ui.FastScrollContentPadding
import com.solthariel.pawchan.core.ui.FastScrollGeometry
import com.solthariel.pawchan.core.ui.FastScrollItemSizes
import com.solthariel.pawchan.core.ui.FastScrollTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FastScrollItemSizesTest {
    @Test
    fun expandedCategoryStartsNearItsRealPositionNotNearTheBottom() {
        val sizes = FastScrollItemSizes(intArrayOf(40, 76) + IntArray(70) { 69 })
        val geometry = FastScrollGeometry(sizes.itemCount, 600, 69f, 0, FastScrollContentPadding(20, 54), sizes)
        val progress = geometry.progress(1, 0, true, true)
        assertEquals(40f / 4420f, progress, 0.00001f)
        assertTrue(progress < 0.02f)
    }

    @Test
    fun visibleAverageCannotChangeAnExactPositionOrTarget() {
        val sizes = FastScrollItemSizes(intArrayOf(40, 58, 58, 76) + IntArray(70) { 69 })
        val small = FastScrollGeometry(sizes.itemCount, 600, 40f, 0, FastScrollContentPadding(20, 54), sizes)
        val large = FastScrollGeometry(sizes.itemCount, 600, 5000f, 0, FastScrollContentPadding(20, 54), sizes)
        assertEquals(small.progress(30, 25, true, true), large.progress(30, 25, true, true), 0f)
        assertEquals(small.target(0.6f), large.target(0.6f))
    }

    @Test
    fun pixelProgressNeverMovesBackwardsAcrossUnequalRows() {
        val sizes = FastScrollItemSizes(intArrayOf(20, 1800, 30, 80, 900, 40, 1500))
        val geometry = FastScrollGeometry(sizes.itemCount, 500, 100f, 0, FastScrollContentPadding(20, 54), sizes)
        val range = sizes.totalHeight + 20 + 54 - 500
        var previous = 0f
        for (pixel in 0L..range) {
            val target = sizes.target(pixel.toDouble())
            val progress = geometry.progress(target.index, target.offset, pixel > 0L, pixel < range)
            assertTrue(progress >= previous)
            assertEquals(pixel.toFloat() / range, progress, 0.0001f)
            previous = progress
        }
    }

    @Test
    fun dragTargetsAndPixelPositionsRoundTrip() {
        val sizes = FastScrollItemSizes(intArrayOf(40, 58, 76) + IntArray(71) { 69 })
        val geometry = FastScrollGeometry(sizes.itemCount, 600, 500f, 0, FastScrollContentPadding(20, 54), sizes)
        val range = sizes.totalHeight + 20 + 54 - 600
        for (step in 0..100) {
            val progress = step / 100f
            val target = geometry.target(progress)
            val distance = sizes.offsetFor(target.index) + target.offset
            assertEquals(range * progress, distance.toFloat(), 1f)
        }
    }

    @Test
    fun prefixSizesOwnTheirInputAndHandleEmptyLists() {
        val source = intArrayOf(40, 76, 69)
        val sizes = FastScrollItemSizes(source)
        source[0] = 9999
        assertEquals(40L, sizes.offsetFor(1))
        assertEquals(76, sizes.heightAt(1))
        assertEquals(FastScrollTarget(1, 10), sizes.target(50.0))
        assertEquals(FastScrollTarget(0, 0), FastScrollItemSizes(intArrayOf()).target(500.0))
    }

    @Test
    fun largePixelTotalsUseLongWithoutOverflow() {
        val sizes = FastScrollItemSizes(IntArray(100000) { 30000 })
        assertEquals(3000000000L, sizes.totalHeight)
        assertEquals(FastScrollTarget(75000, 0), sizes.target(2250000000.0))
    }
}
