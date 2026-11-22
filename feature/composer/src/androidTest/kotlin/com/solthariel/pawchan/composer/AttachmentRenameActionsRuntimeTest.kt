package com.solthariel.pawchan.composer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttachmentRenameActionsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun trashDisablesOnlyTheNameAndRestoresTheEnteredValueOnSecondTap() {
        val file = ComposerAttachment(ReplyAttachment("source.png", "image/png", byteArrayOf(1)))
        val current = mutableStateOf(file.copy(options = file.options.copy(customName = "my-name")))
        compose.setContent {
            MaterialTheme { AttachmentRenameControls(current.value, true) { current.value = it } }
        }
        compose.onNodeWithContentDescription("Убрать имя файла").performClick()
        compose.onNodeWithText("my-name").assertIsNotEnabled()
        compose.runOnIdle {
            assertTrue(current.value.options.removeFileName)
            assertEquals("my-name", current.value.options.customName)
            assertEquals(file.source, current.value.source)
        }
        compose.onNodeWithContentDescription("Убрать имя файла").assertIsEnabled().performClick()
        compose.onNodeWithText("my-name").assertIsEnabled()
    }

    @Test fun resetClearsTheNameAndTrashTogetherEvenWithoutACustomName() {
        val file = ComposerAttachment(ReplyAttachment("source.png", "image/png", byteArrayOf(1)))
        val current = mutableStateOf(file.copy(options = file.options.copy(customName = "my-name", spoiler = true)))
        compose.setContent {
            MaterialTheme { AttachmentRenameControls(current.value, true) { current.value = it } }
        }
        compose.onNodeWithContentDescription("Убрать имя файла").performClick()
        compose.onNodeWithContentDescription("Вернуть исходное имя").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertFalse(current.value.options.removeFileName)
            assertNull(current.value.options.customName)
            assertTrue(current.value.options.spoiler)
        }
        compose.onNodeWithText("source").assertIsEnabled()
        compose.onNodeWithContentDescription("Вернуть исходное имя").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Убрать имя файла").performClick()
        compose.onNodeWithContentDescription("Вернуть исходное имя").assertIsEnabled().performClick()
        compose.runOnIdle { assertFalse(current.value.options.removeFileName) }
    }
}
