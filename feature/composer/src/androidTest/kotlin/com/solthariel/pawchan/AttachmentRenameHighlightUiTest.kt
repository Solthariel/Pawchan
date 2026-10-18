package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
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
        compose.onNodeWithText("Переименовать").performTextReplacement("newname")
        assertRenameState("Включено")
        compose.onNodeWithText("Переименовать").performTextReplacement("image")
        assertRenameState("Выключено")
        compose.onNodeWithText("Переименовать").performTextReplacement("newname")
        compose.onNodeWithContentDescription("Вернуть исходное имя").performClick()
        assertRenameState("Выключено")
        compose.onNodeWithText("Переименовать").performTextReplacement("newname")
        compose.onNodeWithContentDescription("Убрать имя файла").performClick()
        compose.onNodeWithContentDescription("Переименовать").assertIsNotEnabled()
        assertRenameState("Выключено")
        compose.onNodeWithContentDescription("Убрать имя файла").performClick()
        assertRenameState("Включено")
    }

    private fun assertRenameState(state: String) {
        compose.onNodeWithContentDescription("Переименовать").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state),
        )
    }
}
