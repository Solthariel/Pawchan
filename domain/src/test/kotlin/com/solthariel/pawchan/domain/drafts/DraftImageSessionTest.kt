package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftImageSessionTest {
    @Test
    fun manyGestureUpdatesBecomeOneUndoAndNewActionClearsRedo() {
        val initial = DraftImageEdit()
        val session = DraftImageSession(initial)
        session.beginGesture()
        session.preview(initial.copy(crop = DraftCrop(.1f, .1f, 1f, 1f)))
        val final = initial.copy(crop = DraftCrop(.2f, .2f, 1f, 1f))
        session.preview(final)
        session.finishGesture()
        session.undo()
        assertEquals(initial, session.current)
        assertFalse(session.canUndo)
        session.redo()
        assertEquals(final, session.current)
        session.undo()
        session.commit(initial.rotateLeft())
        assertFalse(session.canRedo)
    }

    @Test
    fun resetIsUndoableAndCancellationDoesNotChangeHistory() {
        val initial = DraftImageEdit(quarterTurns = 1)
        val session = DraftImageSession(initial)
        session.reset()
        assertEquals(DraftImageEdit(), session.current)
        session.undo()
        assertEquals(initial, session.current)
        session.beginGesture()
        session.preview(initial.flipHorizontal())
        session.cancelGesture()
        assertEquals(initial, session.current)
        assertTrue(session.canRedo)
        session.redo()
        assertEquals(DraftImageEdit(), session.current)
    }

    @Test
    fun undoDuringGestureCancelsPreviewWithoutUndoingPriorAction() {
        val session = DraftImageSession(DraftImageEdit())
        val rotated = DraftImageEdit().rotateLeft()
        session.commit(rotated)
        session.beginGesture()
        session.preview(rotated.flipVertical())
        session.undo()
        assertEquals(rotated, session.current)
        session.undo()
        assertEquals(DraftImageEdit(), session.current)
    }

    @Test
    fun rejectsUnbalancedGestureCallsAndCopiesMutableInput() {
        val session = DraftImageSession(DraftImageEdit())
        assertThrows(IllegalStateException::class.java) { session.finishGesture() }
        val points = mutableListOf(DraftPoint(.1f, .1f))
        val mark = DraftStroke(points, -1, .1f)
        session.beginGesture()
        assertThrows(IllegalStateException::class.java) { session.beginGesture() }
        session.preview(DraftImageEdit(marks = listOf(mark)))
        points.clear()
        assertEquals(1, (session.current.marks.single() as DraftStroke).points.size)
        session.finishGesture()
        assertEquals(1, (session.current.marks.single() as DraftStroke).points.size)
    }
}
