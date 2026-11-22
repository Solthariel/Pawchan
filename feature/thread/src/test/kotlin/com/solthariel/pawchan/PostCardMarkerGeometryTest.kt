package com.solthariel.pawchan

import com.solthariel.pawchan.thread.postMarkerSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostCardMarkerGeometryTest {
    @Test fun equalSegmentsFillTheSameBoundsAtDifferentHeightsAndDensities() {
        for (density in listOf(1f, 1.5f, 2.75f, 3f)) {
            for (heightDp in listOf(12f, 25f, 67f, 105f, 188f, 317f, 1400f)) {
                val height = heightDp * density
                val segments = postMarkerSegments(height, density)
                assertTrue(segments.count >= 2)
                assertTrue(segments.length > 0f)
                assertEquals(4f * density, segments.gap, 0.001f)
                val end = segments.count * segments.length + (segments.count - 1) * segments.gap
                assertEquals(height, end, 0.001f)
            }
        }
    }

    @Test fun tinyMarkersKeepTwoWholeSegmentsAndOneVisibleGap() {
        val segments = postMarkerSegments(3f, 3f)
        assertEquals(2, segments.count)
        assertEquals(1f, segments.gap, 0.001f)
        assertEquals(1f, segments.length, 0.001f)
    }

    @Test fun emptyOrNegativeHeightDoesNotDraw() {
        for (height in listOf(0f, -10f)) {
            val segments = postMarkerSegments(height, 2f)
            assertEquals(0, segments.count)
            assertEquals(0f, segments.length, 0f)
        }
    }

    @Test fun aKnownHeightHasNoTruncatedLastSegment() {
        val segments = postMarkerSegments(108f, 1f)
        assertEquals(4, segments.count)
        assertEquals(24f, segments.length, 0.001f)
        assertEquals(4f, segments.gap, 0.001f)
    }
}
