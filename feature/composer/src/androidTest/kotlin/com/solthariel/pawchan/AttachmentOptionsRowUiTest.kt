package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.composer.AttachmentOptionsPanel
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttachmentOptionsRowUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun imageOptionsStayOnOneRow() {
        val file = mutableStateOf(image())
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(280.dp, 700.dp)) {
                    AttachmentOptionsPanel(file.value, true, Modifier.testTag("options")) { file.value = it }
                }
            }
        }
        val labels =
            listOf(
                "Уникальный хеш",
                "Удалить метаданные",
                "Убрать имя файла",
                "Спойлер",
                "Перекодировать изображение",
                "Переименовать",
            )
        val panel = compose.onNodeWithTag("options").fetchSemanticsNode().boundsInRoot
        val bounds =
            labels.map {
                val node = compose.onNodeWithContentDescription(it).assertIsDisplayed()
                node.fetchSemanticsNode().boundsInRoot.also { rect ->
                    assertTrue(rect.left >= panel.left - 0.5f)
                    assertTrue(rect.right <= panel.right + 0.5f)
                }
            }
        bounds.forEach { rect ->
            assertTrue(rect.width > 0f)
            assertEquals(bounds.first().top, rect.top, 0.5f)
        }
        bounds.zipWithNext().forEach { (first, second) ->
            assertTrue(first.right <= second.left + 0.5f)
        }
        compose.onNodeWithContentDescription("Спойлер").performClick()
        compose.runOnIdle { assertTrue(file.value.options.spoiler) }
    }

    @Test fun videoShowsOnlyItsOwnOptions() {
        val bytes = byteArrayOf(0x1a, 0x45, 0xdf.toByte(), 0xa3.toByte())
        val file = ComposerAttachment(ReplyAttachment("video.webm", "video/webm", bytes))
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(280.dp, 700.dp)) {
                    AttachmentOptionsPanel(file, true, Modifier.testTag("options")) {}
                }
            }
        }
        compose.onNodeWithContentDescription("Уникальный хеш").assertExists()
        compose.onNodeWithContentDescription("Спойлер").assertExists()
        compose.onNodeWithContentDescription("Убрать имя файла").assertExists()
        compose.onNodeWithContentDescription("Удалить метаданные").assertDoesNotExist()
        compose.onNodeWithContentDescription("Перекодировать изображение").assertDoesNotExist()
        val panel = compose.onNodeWithTag("options").fetchSemanticsNode().boundsInRoot
        val bounds =
            listOf("Уникальный хеш", "Убрать имя файла", "Спойлер", "Переименовать")
                .map {
                    val node = compose.onNodeWithContentDescription(it).assertIsDisplayed()
                    node.fetchSemanticsNode().boundsInRoot.also { rect ->
                        assertTrue(rect.left >= panel.left - 0.5f)
                        assertTrue(rect.right <= panel.right + 0.5f)
                    }
                }
        bounds.forEach { rect ->
            assertTrue(rect.width > 0f)
            assertEquals(bounds.first().top, rect.top, 0.5f)
        }
        bounds.zipWithNext().forEach { (first, second) ->
            assertTrue(first.right <= second.left + 0.5f)
        }
    }

    @Test fun busyEditorDisablesOptionRows() {
        compose.setContent {
            MaterialTheme { AttachmentOptionsPanel(image(), false) {} }
        }
        compose.onNodeWithContentDescription("Уникальный хеш").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Спойлер").assertIsNotEnabled()
    }

    @Test fun removingNameDisablesRenameAction() {
        val file = mutableStateOf(image())
        compose.setContent {
            MaterialTheme { AttachmentOptionsPanel(file.value, true, Modifier.testTag("options")) { file.value = it } }
        }
        compose.onNodeWithContentDescription("Убрать имя файла").performClick()
        compose.onNodeWithContentDescription("Переименовать").assertIsNotEnabled()
    }

    private fun image(): ComposerAttachment = ComposerAttachment(ReplyAttachment("image.jpg", "image/jpeg", byteArrayOf(-1, -40)))
}
