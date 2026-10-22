package com.solthariel.pawchan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import com.solthariel.pawchan.thread.PreviewSwipeConnection
import com.solthariel.pawchan.thread.PreviewSwipeState
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewSwipeConnectionTest {
    @Test fun normalListScrollNeverMovesTheWindow() {
        val state = PreviewSwipeState()
        val connection = PreviewSwipeConnection(state, 1f, {}, {})
        assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, -100f), NestedScrollSource.UserInput))
        assertEquals(Offset.Zero, connection.onPostScroll(Offset(0f, -100f), Offset.Zero, NestedScrollSource.UserInput))
        assertEquals(0f, state.offset, 0f)
    }

    @Test fun reachingTheEdgeHandsOffOnlyTheRemainderOfTheSameGesture() {
        val state = PreviewSwipeState()
        val connection = PreviewSwipeConnection(state, 1f, {}, {})
        val used = connection.onPostScroll(Offset(0f, -200f), Offset(0f, -40f), NestedScrollSource.UserInput)
        assertEquals(Offset(0f, -40f), used)
        assertEquals(-40f, state.offset, 0f)
    }

    @Test fun reversingRestoresWindowAndLeavesUnusedTravelForTheList() {
        val state = PreviewSwipeState()
        val connection = PreviewSwipeConnection(state, 1f, {}, {})
        connection.onPostScroll(Offset.Zero, Offset(0f, 120f), NestedScrollSource.UserInput)
        val used = connection.onPreScroll(Offset(0f, -200f), NestedScrollSource.UserInput)
        assertEquals(Offset(0f, -120f), used)
        assertEquals(-80f, -200f - used.y, 0f)
        assertEquals(0f, state.offset, 0f)
        assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, -80f), NestedScrollSource.UserInput))
    }

    @Test fun inertialFlingCannotAccumulateAPullEvenAfterReachingTheEdge() {
        val state = PreviewSwipeState()
        val connection = PreviewSwipeConnection(state, 1f, {}, {})
        assertEquals(Offset.Zero, connection.onPostScroll(Offset.Zero, Offset(0f, 500f), NestedScrollSource.SideEffect))
        state.drag(100f)
        assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, 500f), NestedScrollSource.SideEffect))
        assertEquals(100f, state.offset, 0f)
    }
}
