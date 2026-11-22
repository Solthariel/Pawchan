package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentViewerDraftTest {
    @Test
    fun imageEditsStayStagedAndSaveIsBlockedOnlyDuringGestures() {
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        assertFalse(draft.changed)
        assertTrue(draft.canSave)
        draft.editor.commit(DraftImageEdit(quarterTurns = 1))
        assertTrue(draft.changed)
        assertTrue(draft.canSave)
        draft.reset()
        assertNull(draft.currentOptions.edit)
        assertTrue(draft.canSave)
        draft.undo()
        assertTrue(draft.canSave)
        draft.redo()
        assertTrue(draft.canSave)
        draft.editor.begin()
        assertFalse(draft.canSave)
        draft.editor.cancel()
        assertTrue(draft.canSave)
    }

    @Test
    fun existingRecipeAndOptionChangesRemainStaged() {
        val options = MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(quarterTurns = 1)))
        val draft = AttachmentViewerDraft(options)
        assertEquals(options, draft.currentOptions)
        assertFalse(draft.changed)
        assertTrue(draft.canSave)
        draft.options = draft.currentOptions.copy(spoiler = true)
        assertTrue(draft.changed)
        assertTrue(draft.canSave)
    }

    @Test
    fun oldTextMarksRemainWhenSwitchingToolsAndChangingCrop() {
        val text = DraftTextMark("Сохранённый текст", DraftPoint(.5f, .5f), DraftTextAppearance(-1, .1f))
        val initial = DraftImageEdit(marks = listOf(text))
        val draft = AttachmentViewerDraft(MediaDraftOptions(image = DraftImageOptions(edit = initial)))
        draft.selectTab(AttachmentEditorTab.CROP)
        draft.editor.commit(initial.copy(quarterTurns = 1))
        draft.selectTab(AttachmentEditorTab.DRAW)
        draft.selectTab(AttachmentEditorTab.REDACT)
        assertEquals(listOf(text), draft.currentOptions.edit?.marks)
        draft.undo()
        assertEquals(initial, draft.currentOptions.edit)
    }

    @Test
    fun switchingModesClearsStaleTextSelection() {
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        draft.selectTab(AttachmentEditorTab.CROP)
        draft.selectedTextIndex = 2
        draft.selectTab(AttachmentEditorTab.DRAW)
        assertNull(draft.selectedTextIndex)
        assertEquals(AttachmentEditorTab.DRAW, draft.tab)
    }

    @Test fun resetAndHistoryChangeOnlyImageEditsNotFileOptions() {
        val options =
            MediaDraftOptions(uniqueHash = true, removeMetadata = true, customName = "retained", spoiler = true)
        val draft = AttachmentViewerDraft(options)
        val turned = DraftImageEdit(quarterTurns = 1)
        draft.editor.commit(turned)
        draft.reset()
        assertEquals(options, draft.currentOptions)
        draft.undo()
        assertEquals(options.copy(image = DraftImageOptions(edit = turned)), draft.currentOptions)
        draft.redo()
        assertEquals(options, draft.currentOptions)
    }

    @Test fun imageHistoryClosesNamePanelButPreservesTheEnteredName() {
        val draft = AttachmentViewerDraft(MediaDraftOptions(customName = "kept"))
        draft.editor.commit(DraftImageEdit(quarterTurns = 1))
        draft.selectTab(AttachmentEditorTab.NAME)
        draft.undo()
        assertEquals(AttachmentEditorTab.FILE, draft.tab)
        assertEquals("kept", draft.currentOptions.customName)
        draft.selectTab(AttachmentEditorTab.NAME)
        draft.redo()
        assertEquals(AttachmentEditorTab.FILE, draft.tab)
        draft.selectTab(AttachmentEditorTab.NAME)
        draft.reset()
        assertEquals(AttachmentEditorTab.FILE, draft.tab)
        assertEquals("kept", draft.currentOptions.customName)
    }
}
