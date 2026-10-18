package com.solthariel.pawchan.core.ui

import com.solthariel.pawchan.core.ui.FastScrollGesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastScrollGestureTest {
    private val gesture = FastScrollGesture().apply { trackHeight = 800f }

    @Test
    fun grabbingTheThumbDoesNotRecenterIt() {
        val top = (800f - 48f) * 0.4f
        gesture.begin(top + 7f, 0.4f, 48f)
        assertEquals(0.4f, gesture.progress, 0.00001f)
        gesture.move(top + 107f, 48f)
        assertEquals(0.4f + 100f / 752f, gesture.progress, 0.00001f)
    }

    @Test
    fun tappingTheTrackCentersTheThumbAtThePointer() {
        gesture.begin(400f, 0f, 48f)
        assertEquals(0.5f, gesture.progress, 0f)
        assertTrue(gesture.dragging)
    }

    @Test
    fun draggingPastEitherEdgeReachesExactEndpoints() {
        gesture.begin(400f, 0f, 48f)
        gesture.move(800f, 48f)
        assertEquals(1f, gesture.progress, 0f)
        gesture.move(-10f, 48f)
        assertEquals(0f, gesture.progress, 0f)
    }

    @Test
    fun releasingOrCancellingRestoresIdleState() {
        gesture.begin(400f, 0f, 48f)
        gesture.finish()
        assertFalse(gesture.dragging)
        assertEquals(1, gesture.interaction)
    }

    @Test
    fun zeroLengthTrackDoesNotDivideByZero() {
        gesture.trackHeight = 48f
        gesture.begin(20f, 0f, 48f)
        assertEquals(0f, gesture.progress, 0f)
    }
}
