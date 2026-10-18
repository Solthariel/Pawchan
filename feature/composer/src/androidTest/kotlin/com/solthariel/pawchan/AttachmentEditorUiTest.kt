package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.composer.AttachmentOption
import com.solthariel.pawchan.composer.AttachmentRenameControls
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import com.solthariel.pawchan.feature.composer.R
import com.solthariel.pawchan.media.AttachmentDialogContent
import com.solthariel.pawchan.media.AttachmentDialogLayout
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
        compose.onNodeWithText("original").performTextClearance()
        compose.onNodeWithText("Переименовать").performTextInput("newname")
        compose.runOnIdle { assertEquals("newname", file.value.options.customName) }
        compose.onNodeWithContentDescription("Вернуть исходное имя").performClick()
        compose.onNodeWithText("original").assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, file.value.options.customName) }
    }

    @Test
    fun wideMediaUsesItsAspectRatioInsteadOfAnOversizedFixedViewport() {
        compose.setContent {
            Box(Modifier.size(360.dp, 700.dp)) {
                AttachmentDialogLayout(
                    AttachmentDialogContent(
                        header = { Box(Modifier.height(70.dp)) },
                        footer = { Box(Modifier.height(304.dp)) },
                        media = { Box(Modifier.testTag("media")) },
                        aspectRatio = 2f,
                    ),
                )
            }
        }
        compose.onNodeWithTag("media").assertHeightIsEqualTo(180.dp)
    }

    @Test
    fun tallPreviewIsCappedAndDoesNotStretchTheDialog() {
        compose.setContent {
            Box(Modifier.size(360.dp, 900.dp)) {
                AttachmentDialogLayout(
                    AttachmentDialogContent(
                        header = { Box(Modifier.height(0.dp)) },
                        footer = { Box(Modifier.height(304.dp)) },
                        media = { Box(Modifier.testTag("media")) },
                        aspectRatio = 0.5f,
                        previewHeightLimit = 240.dp,
                    ),
                )
            }
        }
        compose.onNodeWithTag("media").assertHeightIsEqualTo(240.dp)
    }
}
