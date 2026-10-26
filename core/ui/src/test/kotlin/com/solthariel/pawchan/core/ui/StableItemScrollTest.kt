package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StableItemScrollTest {
    @Test fun thousandsOfSlowPointerStepsNeverReverseTheSelectedPost() {
        var previous = 0
        repeat(10_001) { step ->
            val index = itemScrollTarget(step / 10_000f, 100).index
            assertTrue("Downward pointer reversed at $step", index >= previous)
            previous = index
        }
        repeat(10_001) { step ->
            val index = itemScrollTarget(1f - step / 10_000f, 100).index
            assertTrue("Upward pointer reversed at $step", index <= previous)
            previous = index
        }
    }

    @Test fun targetDoesNotOscillateWhenNeighbouringRowsChangeTheViewport() {
        var first = 58
        repeat(100) { event ->
            // The old scale used 2 complete rows at 58 and 3 at 59, feeding its own result back.
            val next = itemScrollTarget(0.6f + event * 0.00001f, 100).index
            assertTrue(next >= first)
            first = next
        }
    }

    @Test fun grabbingAnyInteriorItemUsesTheSameInverseScale() {
        for (count in listOf(2, 17, 100, 1001)) {
            for (index in 0 until count) {
                val progress = itemScrollProgress(index, count, true, true)
                assertEquals(index, itemScrollTarget(progress, count).index)
            }
        }
    }
}
