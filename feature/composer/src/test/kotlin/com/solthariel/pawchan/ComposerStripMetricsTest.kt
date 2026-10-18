package com.solthariel.pawchan

import com.solthariel.pawchan.composer.calculateComposerStripMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerStripMetricsTest {
    @Test
    fun oneToFourFilesUseOneRowWithoutChangingStripHeight() {
        val first = calculateComposerStripMetrics(1, 360f)
        for (count in 1..4) {
            val metrics = calculateComposerStripMetrics(count, 360f)
            assertEquals(1, metrics.rows)
            assertEquals(first.totalHeight, metrics.totalHeight, 0f)
            assertEquals(metrics.totalHeight, metrics.cellHeight, 0f)
        }
    }

    @Test
    fun fiveToEightFilesUseTwoRowsWithinTheSameOverallHeight() {
        val single = calculateComposerStripMetrics(4, 360f)
        for (count in 5..8) {
            val metrics = calculateComposerStripMetrics(count, 360f)
            assertEquals(2, metrics.rows)
            assertEquals(single.totalHeight, metrics.totalHeight, 0f)
            assertEquals(metrics.totalHeight, metrics.cellHeight * 2 + 6f, 0f)
        }
    }

    @Test
    fun narrowWidthNeverProducesNegativeCellHeight() {
        assertTrue(calculateComposerStripMetrics(8, 10f).cellHeight >= 0f)
    }
}
