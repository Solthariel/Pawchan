package com.solthariel.pawchan

import com.solthariel.pawchan.thread.PreviewSwipeState
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSwipeStateTest {
    @Test fun windowTracksTheFingerAndConsumesItsDelta() {
        val state = PreviewSwipeState()
        assertEquals(120f, state.drag(120f), 0f)
        assertEquals(120f, state.offset, 0f)
        assertEquals(40f, state.drag(40f), 0f)
        assertEquals(160f, state.offset, 0f)
    }

    @Test fun returningToZeroOnlyConsumesRequiredRawTravelWithoutCrossingOver() {
        val state = PreviewSwipeState()
        state.drag(120f)
        assertEquals(-120f, state.drag(-200f), 0f)
        assertEquals(0f, state.offset, 0f)
    }

    @Test fun partialReverseDoesNotMultiplyMotionOrSwallowTheRemainder() {
        val state = PreviewSwipeState()
        state.drag(-200f)
        assertEquals(40f, state.drag(40f), 0f)
        assertEquals(-160f, state.offset, 0f)
        assertEquals(160f, state.drag(300f), 0f)
        assertEquals(0f, state.offset, 0f)
    }

    @Test fun cancellationResetsMotionAndTheNextGestureStartsClean() {
        val state = PreviewSwipeState()
        state.beginGesture()
        state.drag(200f)
        state.cancelGesture()
        assertEquals(0f, state.offset, 0f)
        state.beginGesture()
        state.drag(-80f)
        assertEquals(-80f, state.offset, 0f)
    }
}
