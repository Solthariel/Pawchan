package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftStroke
import com.solthariel.pawchan.domain.drafts.sourceToCanvas
import org.junit.Assert.assertEquals
import org.junit.Test

class ImageEraserGestureTest {
    @Test
    fun eraserRemovesWholeMarksAcrossAllTransformsAndUndoRestoresThem() {
        for (turn in 0..3) {
            for (mirror in listOf(false, true)) {
                val mark = DraftStroke(listOf(DraftPoint(.2f, .3f), DraftPoint(.8f, .3f)), -1, .02f)
                val edit = DraftImageEdit(quarterTurns = turn, mirrored = mirror, marks = listOf(mark))
                val state = ImageEditorState(edit)
                val gesture = ImageEraserGesture(edit, 400, 200)
                val point = edit.sourceToCanvas(DraftPoint(.5f, .3f))
                state.begin()
                state.preview(gesture.erase(point.x, point.y, .03f))
                state.finish()
                assertEquals(emptyList<Any>(), state.edit.marks)
                state.undo()
                assertEquals(edit, state.edit)
                state.redo()
                assertEquals(emptyList<Any>(), state.edit.marks)
            }
        }
    }

    @Test
    fun cancelledEraserGestureKeepsDrawing() {
        val mark = DraftStroke(listOf(DraftPoint(.5f, .5f)), -1, .02f)
        val edit = DraftImageEdit(marks = listOf(mark))
        val state = ImageEditorState(edit)
        val gesture = ImageEraserGesture(edit, 100, 100)
        state.begin()
        state.preview(gesture.erase(.5f, .5f, .03f))
        state.cancel()
        assertEquals(edit, state.edit)
        assertEquals(false, state.canUndo)
    }
}
