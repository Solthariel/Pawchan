package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftMarkGesture
import com.solthariel.pawchan.domain.drafts.DraftMarkTool
import com.solthariel.pawchan.domain.drafts.DraftPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEditorStateTest {
    @Test
    fun allToolsShareOneOrderedUndoHistory() {
        val state = ImageEditorState(DraftImageEdit())
        state.commit(state.edit.rotateLeft())
        val rotated = state.edit
        val crop = state.cropActions()
        crop.onStart()
        crop.onChange(state.edit.copy(crop = DraftCrop(.1f, .1f, .9f, .9f)))
        crop.onFinish()
        val cropped = state.edit
        val pen = DraftMarkTool.Pen(-1, .1f)
        val drawing = requireNotNull(DraftMarkGesture.begin(state.edit, pen, DraftPoint(.5f, .5f)))
        val mark = state.markActions { error("unexpected limit") }
        mark.onStart()
        mark.onChange(drawing.snapshot())
        mark.onChange(drawing.update(DraftPoint(.6f, .6f)))
        mark.onFinish()
        assertEquals(1, state.edit.marks.size)
        state.undo()
        assertEquals(cropped, state.edit)
        state.undo()
        assertEquals(rotated, state.edit)
        state.undo()
        assertEquals(DraftImageEdit(), state.edit)
        assertFalse(state.canUndo)
        assertTrue(state.canRedo)
        state.redo()
        assertEquals(rotated, state.edit)
    }

    @Test
    fun gestureSnapshotRemainsStableUntilFinishOrCancel() {
        val state = ImageEditorState(DraftImageEdit())
        val initial = state.edit
        val gesture = requireNotNull(DraftMarkGesture.begin(initial, DraftMarkTool.Pen(-1, .1f), DraftPoint(.2f, .2f)))
        state.begin()
        state.preview(gesture.update(DraftPoint(.4f, .4f)))
        assertEquals(initial, state.gestureStart)
        state.finish()
        assertEquals(null, state.gestureStart)
        state.begin()
        assertEquals(state.edit, state.gestureStart)
        state.cancel()
        assertEquals(null, state.gestureStart)
    }

    @Test
    fun cancelledGestureDoesNotChangeHistoryAndResetIsUndoable() {
        val initial = DraftImageEdit(quarterTurns = 2)
        val state = ImageEditorState(initial)
        val actions = state.markActions { error("unexpected limit") }
        actions.onStart()
        state.undo()
        state.reset()
        assertTrue(state.inGesture)
        actions.onChange(initial.flipHorizontal())
        actions.onCancel()
        assertEquals(initial, state.edit)
        assertFalse(state.canUndo)
        assertFalse(state.inGesture)
        state.reset()
        assertEquals(DraftImageEdit(), state.edit)
        state.undo()
        assertEquals(initial, state.edit)
    }
}
