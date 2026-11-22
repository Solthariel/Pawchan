package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftImageHistoryTest {
    @Test
    fun cropTransformsAndResetAreAllUndoableAndNewEditClearsRedo() {
        val original = DraftImageEdit()
        val history = DraftImageHistory(original)
        val crop = original.copy(crop = DraftCrop(0.1f, 0f, 1f, 1f))
        val rotated = crop.rotateLeft()
        history.commit(crop)
        history.commit(rotated)
        history.commit(original)
        history.undo()
        assertEquals(rotated, history.current)
        history.undo()
        assertEquals(crop, history.current)
        history.redo()
        assertEquals(rotated, history.current)
        history.commit(crop.flipHorizontal())
        assertFalse(history.canRedo)
    }

    @Test
    fun historyOwnsADeepCopyOfMutableInputLists() {
        val points = mutableListOf(DraftPoint(0.2f, 0.3f))
        val marks = mutableListOf<DraftImageMark>(DraftStroke(points, -1, 0.1f))
        val history = DraftImageHistory(DraftImageEdit(marks = marks))
        points.clear()
        marks.clear()
        assertEquals(1, history.current.marks.size)
        assertEquals(1, (history.current.marks.single() as DraftStroke).points.size)
    }

    @Test
    fun historyIsBoundedAndIdenticalCommitDoesNotDestroyRedo() {
        val original = DraftImageEdit()
        val history = DraftImageHistory(original, capacity = 2)
        val a = original.rotateLeft()
        val b = a.rotateLeft()
        val c = b.rotateLeft()
        history.commit(a)
        history.commit(b)
        history.commit(c)
        history.undo()
        history.commit(b)
        assertTrue(history.canRedo)
        history.undo()
        assertEquals(a, history.current)
        assertFalse(history.canUndo)
    }
}
