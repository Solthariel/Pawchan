package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FastScrollItemNavigationTest {
    @Test fun midpointSnapsToTheMiddlePostIndependentOfVisibleCount() {
        assertEquals(FastScrollTarget(50, 0), itemScrollTarget(0.5f, 100))
    }

    @Test fun endpointsReachFirstAndLastPost() {
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0f, 100))
        assertEquals(FastScrollTarget(99, 0), itemScrollTarget(1f, 100))
    }

    @Test fun tallPostWithoutFullyVisibleRowsRemainsReachable() {
        assertEquals(FastScrollTarget(50, 0), itemScrollTarget(0.5f, 100))
    }

    @Test fun emptySingleAndOversizedVisibleCountAreSafe() {
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0.5f, 0))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0.5f, 1))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0.5f, 1))
    }

    @Test fun outOfRangeAndNonFinitePointerValuesAreSafe() {
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(-1f, 100))
        assertEquals(FastScrollTarget(99, 0), itemScrollTarget(2f, 100))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(Float.NaN, 100))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(Float.POSITIVE_INFINITY, 100))
    }

    @Test fun ordinaryItemProgressReflectsIndexNotCardHeight() {
        assertEquals(0.5f, itemScrollProgress(50, 101, true, true), 0f)
        assertEquals(0f, itemScrollProgress(0, 100, true, true), 0f)
    }

    @Test fun trueScrollableEdgesOverrideIndexEstimate() {
        assertEquals(0f, itemScrollProgress(5, 100, false, true), 0f)
        assertEquals(1f, itemScrollProgress(96, 100, true, false), 0f)
        assertEquals(0f, itemScrollProgress(0, 0, false, false), 0f)
    }

    @Test fun defaultModeStillUsesExistingContentOffsets() {
        val geometry = FastScrollGeometry(100, 740, 110f, 6, FastScrollContentPadding(6, 6))
        assertEquals(FastScrollTarget(46, 97), geometry.target(0.5f))
        assertEquals(FastScrollTarget(50, 0), itemScrollTarget(0.5f, 100))
    }
}
