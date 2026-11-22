package com.solthariel.pawchan.media

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaOpeningGeometryTest {
    private val origin = Rect(120f, 250f, 220f, 350f)
    private val viewer = Rect(20f, 50f, 420f, 850f)

    @Test
    fun startsAtThumbnailPositionAcrossDifferentWindows() {
        assertEquals(Rect(100f, 200f, 200f, 300f), openingFrame(origin, viewer, 800f, 400f, 0f))
    }

    @Test
    fun endsAtTheSameFittedImageBoundsAsViewer() {
        assertEquals(Rect(0f, 300f, 400f, 500f), openingFrame(origin, viewer, 800f, 400f, 1f))
        assertEquals(Rect(100f, 0f, 300f, 800f), openingFrame(origin, viewer, 400f, 1600f, 1f))
    }

    @Test
    fun progressIsClampedAndFramesInterpolate() {
        val start = openingFrame(origin, viewer, 800f, 400f, -1f)
        val end = openingFrame(origin, viewer, 800f, 400f, 2f)
        assertEquals(Rect(100f, 200f, 200f, 300f), start)
        assertEquals(Rect(0f, 300f, 400f, 500f), end)
        assertEquals(Rect(50f, 250f, 300f, 400f), openingFrame(origin, viewer, 800f, 400f, 0.5f))
    }

    @Test
    fun invalidOrOffscreenOriginFallsBackToViewerWithoutTransition() {
        assertNull(openingFrame(Rect.Zero, viewer, 800f, 400f, 0f))
        assertNull(openingFrame(Rect(900f, 900f, 1000f, 1000f), viewer, 800f, 400f, 0f))
        assertNull(openingFrame(origin, Rect.Zero, 800f, 400f, 0f))
        assertNull(openingFrame(origin, viewer, 0f, 400f, 0f))
    }

    @Test
    fun verySmallOriginalIsLimitedLikeTheViewer() {
        // Initial viewer zoom cannot exceed eight times the source pixels.
        assertEquals(Rect(160f, 360f, 240f, 440f), openingFrame(origin, viewer, 10f, 10f, 1f))
    }
}
