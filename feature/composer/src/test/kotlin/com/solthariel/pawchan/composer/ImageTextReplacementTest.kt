package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ImageTextReplacementTest {
    @Test
    fun replacementIsImmutableAndUndoable() {
        val original = DraftTextMark("Before", DraftPoint(.5f, .5f), DraftTextAppearance(-1, .1f))
        val edit = DraftImageEdit(marks = listOf(original))
        val updated = original.copy(text = "After")
        val state = ImageEditorState(edit)
        state.commit(state.edit.replaceText(0, updated))
        assertEquals(updated, state.edit.marks.single())
        assertEquals(original, edit.marks.single())
        state.undo()
        assertEquals(edit, state.edit)
        state.redo()
        assertEquals(updated, state.edit.marks.single())
        assertSame(edit, edit.replaceText(5, updated))
    }
}
