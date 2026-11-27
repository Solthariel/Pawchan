package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.solthariel.pawchan.composer.AttachmentOption
import com.solthariel.pawchan.composer.AttachmentRenameControls
import com.solthariel.pawchan.composer.AttachmentRenameDialog
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.feature.composer.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttachmentEditorUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tileTogglesOnlyOnce() {
        val checked = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                AttachmentOption(R.string.attachment_unique_hash, checked.value, true) { checked.value = it }
            }
        }
        compose.onNodeWithContentDescription("Уникальный хеш").performClick()
        compose.runOnIdle { assertTrue(checked.value) }
        compose.onNodeWithContentDescription("Уникальный хеш").performClick()
        compose.runOnIdle { assertFalse(checked.value) }
    }

    @Test
    fun filenameIsEditableWithoutRenameCheckboxAndCanBeRestored() {
        val file = mutableStateOf(ComposerAttachment(ReplyAttachment("original.png", "image/png", byteArrayOf(1))))
        compose.setContent { MaterialTheme { AttachmentRenameControls(file.value, true) { file.value = it } } }
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("newname")
        compose.runOnIdle { assertEquals("newname", file.value.options.customName) }
        compose.onNodeWithContentDescription("Вернуть исходное имя").performClick()
        compose.onNodeWithText("original").assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, file.value.options.customName) }
    }

    @Test
    fun renameCancelDiscardsAndSaveCommitsDraft() {
        val initial = ComposerAttachment(ReplyAttachment("original.png", "image/png", byteArrayOf(1)))
        val file = mutableStateOf(initial)
        val open = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (open.value) AttachmentRenameDialog(file.value, true, { file.value = it }) { open.value = false }
            }
        }
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("newname")
        compose.runOnIdle { assertEquals(null, file.value.options.customName) }
        val cancel =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(android.R.string.cancel)
        compose.onNodeWithText(cancel).performClick()
        compose.runOnIdle {
            assertEquals(null, file.value.options.customName)
            open.value = true
        }
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("saved")
        compose.onNodeWithText("Сохранить").performClick()
        compose.runOnIdle { assertEquals("saved", file.value.options.customName) }
    }
}
