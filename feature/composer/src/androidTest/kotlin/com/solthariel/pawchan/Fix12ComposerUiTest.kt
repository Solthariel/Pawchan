package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.composer.AttachmentEditorFooter
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerIdentityFields
import com.solthariel.pawchan.domain.posting.PostingIcon
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class Fix12ComposerUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun choosingAndClearingBoardIconPreservesOtherFields() {
        val draft = mutableStateOf(ComposerDraftState(email = "test@example.test"))
        compose.setContent {
            Column {
                ComposerIdentityFields(
                    draft.value,
                    PostingRules(
                        identity =
                            PostingIdentityRules(
                                icons = listOf(PostingIcon(7, "Рено"), PostingIcon(1, "Альфа Ромео")),
                            ),
                    ),
                    true,
                    onChange = { draft.value = it },
                )
            }
        }
        compose.onNodeWithText("Нет иконки").performClick()
        compose.onNodeWithText("Рено").performClick()
        assertEquals(7, draft.value.options.icon)
        assertEquals("test@example.test", draft.value.email)
        compose.onNodeWithText("Рено").performClick()
        compose.onNodeWithText("Нет иконки").performClick()
        assertEquals(null, draft.value.options.icon)
    }

    @Test
    fun attachmentFooterHasOnlyRemoveAndNoConfirmation() {
        var removed = 0
        val file = ComposerAttachment(ReplyAttachment("test.jpg", "image/jpeg", byteArrayOf(1)))
        compose.setContent {
            AttachmentEditorFooter(file, true, {}, { removed++ })
        }
        compose.onNodeWithText("Применить").assertDoesNotExist()
        compose.onNodeWithText("Удалить вложение").assertIsDisplayed().performClick()
        assertEquals(1, removed)
    }
}
