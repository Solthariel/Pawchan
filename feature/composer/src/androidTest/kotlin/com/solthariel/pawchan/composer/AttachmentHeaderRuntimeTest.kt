package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttachmentHeaderRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowMenuAppliesResetAndRemovalWithoutConfirmation() {
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        draft.editor.commit(DraftImageEdit(quarterTurns = 1))
        var removed = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(232.dp)) {
                    AttachmentViewerHeader(true, {}, { removed++ }, draft)
                }
            }
        }
        compose.onNodeWithText("Вложение").assertDoesNotExist()
        compose.onNodeWithContentDescription("Ещё").performClick()
        compose.onNodeWithText("Сбросить правки").performClick()
        compose.onNodeWithText("Сбросить правки?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(DraftImageEdit(), draft.editor.edit) }
        compose.onNodeWithContentDescription("Ещё").performClick()
        compose.onNodeWithText("Удалить вложение").performClick()
        compose.onNodeWithText("Удалить вложение?").assertDoesNotExist()
        compose.runOnIdle { assertTrue(removed == 1) }
    }
}
