package com.solthariel.pawchan.core.ui

import com.solthariel.pawchan.core.ui.FastScrollContentPadding
import com.solthariel.pawchan.core.ui.FastScrollGeometry
import com.solthariel.pawchan.core.ui.FastScrollTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FastScrollGeometryTest {
    private val geometry = FastScrollGeometry(100, 740, 110f, 6, FastScrollContentPadding(6, 6))

    @Test
    fun includesSpacingPaddingAndPartialRows() {
        // Content = 100 * 110 + 99 * 6 + 12 = 11606; range = 10866.
        val progress = geometry.progress(90, 50, canScrollBackward = true, canScrollForward = true)
        assertEquals(10490f / 10866f, progress, 0.00001f)
        assertTrue(progress < 1f)
    }

    @Test
    fun doesNotSaturateAtTheOldVisibleCountEndpoint() {
        // Seven visible rows (including clipped rows) used to produce index 100 - 7.
        val progress = geometry.progress(93, 0, canScrollBackward = true, canScrollForward = true)
        assertTrue(progress < 1f)
        val remainingPixels = 10866 - 93 * 116
        assertEquals(78, remainingPixels)
    }

    @Test
    fun exactBoundariesComeFromActualScrollability() {
        assertEquals(0f, geometry.progress(0, 0, false, true), 0f)
        assertEquals(1f, geometry.progress(93, 78, true, false), 0f)
        assertEquals(0f, geometry.progress(0, 0, false, false), 0f)
    }

    @Test
    fun thumbDoesNotReachAnEndpointFromAnEstimateAlone() {
        assertTrue(geometry.progress(99, 500, true, true) < 1f)
        assertTrue(geometry.progress(0, 0, true, true) > 0f)
    }

    @Test
    fun intermediateTargetsIncludeSubRowOffsets() {
        for (step in 1 until 100) {
            val progress = step / 100f
            val target = geometry.target(progress)
            val actualPixels = target.index * 116 + target.offset
            assertEquals(10866 * progress, actualPixels.toFloat(), 1f)
        }
    }

    @Test
    fun emptyAndShortListsHaveSafeTargets() {
        assertEquals(
            FastScrollTarget(0, 0),
            FastScrollGeometry(0, 740, 0f, 6, FastScrollContentPadding(6, 6)).target(1f),
        )
        assertEquals(
            FastScrollTarget(0, 0),
            FastScrollGeometry(2, 740, 110f, 6, FastScrollContentPadding(6, 6)).target(1f),
        )
    }

    @Test
    fun largeListsDoNotOverflowTheirPixelProjection() {
        val large = FastScrollGeometry(100000, 740, 30000f, 6, FastScrollContentPadding(6, 6))
        val target = large.target(0.75f)
        assertTrue(target.index in 74998..75000)
        assertTrue(target.offset in 0..30005)
    }
}
