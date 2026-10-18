package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FastScrollItemNavigationTest {
    @Test fun midpointSnapsToPostStartAccountingForFullyVisiblePosts() {
        assertEquals(FastScrollTarget(48, 0), itemScrollTarget(0.5f, 100, 4))
    }

    @Test fun endpointsReachFirstAndLastPost() {
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0f, 100, 4))
        assertEquals(FastScrollTarget(99, 0), itemScrollTarget(1f, 100, 4))
    }

    @Test fun tallPostWithoutFullyVisibleRowsRemainsReachable() {
        assertEquals(FastScrollTarget(50, 0), itemScrollTarget(0.5f, 100, 0))
    }

    @Test fun emptySingleAndOversizedVisibleCountAreSafe() {
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0.5f, 0, 0))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0.5f, 1, 1))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(0.5f, 3, 10))
    }

    @Test fun outOfRangeAndNonFinitePointerValuesAreSafe() {
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(-1f, 100, 4))
        assertEquals(FastScrollTarget(99, 0), itemScrollTarget(2f, 100, 4))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(Float.NaN, 100, 4))
        assertEquals(FastScrollTarget(0, 0), itemScrollTarget(Float.POSITIVE_INFINITY, 100, 4))
    }

    @Test fun ordinaryItemProgressReflectsIndexNotCardHeight() {
        assertEquals(0.5f, itemScrollProgress(48, 100, 4, true, true), 0f)
        assertEquals(0f, itemScrollProgress(0, 100, 0, true, true), 0f)
    }

    @Test fun trueScrollableEdgesOverrideIndexEstimate() {
        assertEquals(0f, itemScrollProgress(5, 100, 4, false, true), 0f)
        assertEquals(1f, itemScrollProgress(96, 100, 4, true, false), 0f)
        assertEquals(0f, itemScrollProgress(0, 0, 0, false, false), 0f)
    }

    @Test fun defaultModeStillUsesExistingContentOffsets() {
        val geometry = FastScrollGeometry(100, 740, 110f, 6, FastScrollContentPadding(6, 6))
        assertEquals(FastScrollTarget(46, 97), geometry.target(0.5f))
        assertEquals(FastScrollTarget(48, 0), itemScrollTarget(0.5f, 100, 4))
    }
}
