package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.solthariel.pawchan.composer.AttachmentOptionsPanel
import com.solthariel.pawchan.composer.AttachmentRenameControls
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Rule
import org.junit.Test

class AttachmentRenameHighlightUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun renameHighlightsAndReturningToOriginalClearsHighlight() {
        val file = mutableStateOf(ComposerAttachment(ReplyAttachment("image.jpg", "image/jpeg", byteArrayOf(-1, -40))))
        compose.setContent {
            MaterialTheme {
                Column {
                    AttachmentRenameControls(file.value, true) { file.value = it }
                    AttachmentOptionsPanel(file.value, true) { file.value = it }
                }
            }
        }
        assertRenameState("Выключено")
        compose.onNodeWithText("Имя файла").performTextReplacement("newname")
        assertRenameState("Включено")
        compose.onNodeWithText("Имя файла").performTextReplacement("image")
        assertRenameState("Выключено")
        compose.onNodeWithText("Имя файла").performTextReplacement("newname")
        compose.onNodeWithContentDescription("Вернуть исходное имя").performClick()
        assertRenameState("Выключено")
        compose.onNodeWithText("Имя файла").performTextReplacement("newname")
        compose
            .onNode(
                hasContentDescription("Убрать имя файла") and hasAnyAncestor(hasTestTag("attachment-rename-controls")),
            ).performClick()
        compose.onNodeWithContentDescription("Переименовать").assertIsNotEnabled()
        assertRenameState("Выключено")
        compose
            .onNode(
                hasContentDescription("Убрать имя файла") and hasAnyAncestor(hasTestTag("attachment-rename-controls")),
            ).performClick()
        assertRenameState("Включено")
    }

    private fun assertRenameState(state: String) {
        compose.onNodeWithContentDescription("Переименовать").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state),
        )
    }
}
