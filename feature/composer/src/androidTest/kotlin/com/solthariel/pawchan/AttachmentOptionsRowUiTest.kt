package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.composer.AttachmentEditorPanel
import com.solthariel.pawchan.composer.AttachmentEditorTab
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

    @Test fun editIsFirstAndTheCaptionIsPartOfItsTouchTarget() {
        var opened = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(304.dp, 700.dp)) {
                    AttachmentOptionsPanel(image(), true, onEditorAction = { opened++ }) {}
                }
            }
        }
        val edit = compose.onNodeWithContentDescription("Изменить").fetchSemanticsNode().boundsInRoot
        val hash = compose.onNodeWithContentDescription("Уникальный хеш").fetchSemanticsNode().boundsInRoot
        assertTrue(edit.right <= hash.left + 0.5f)
        compose.onNodeWithText("Изменить", useUnmergedTree = true).performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, opened) }
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

    @Test fun viewerCombinesNameActionsAndOpensImageEditor() {
        val original = image()
        val draft =
            com.solthariel.pawchan.composer
                .AttachmentViewerDraft(original.options)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 184.dp)) {
                    AttachmentEditorPanel(
                        original.copy(options = draft.currentOptions),
                        draft,
                        com.solthariel.pawchan.composer
                            .ReencodingDimensions(100, 100),
                        true,
                    )
                }
            }
        }
        val name = compose.onNodeWithContentDescription("Переименовать")
        val off = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Выключено")
        val on = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Включено")
        name.assert(off).performClick()
        name.assert(on)
        compose.onAllNodesWithContentDescription("Убрать имя файла").assertCountEquals(1)
        name.performClick()
        name.assert(off)
        compose.onNodeWithContentDescription("Убрать имя файла").assertDoesNotExist()
        name.performClick()
        compose.onNodeWithText("Сохранить").assertDoesNotExist()
        compose.onNodeWithText("Отмена").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertIsNotFocused().performTextReplacement("newname")
        name.performClick()
        name.assert(on).performClick()
        compose
            .onNode(
                hasContentDescription("Убрать имя файла") and hasAnyAncestor(hasTestTag("attachment-rename-controls")),
            ).performClick()
        compose.runOnIdle {
            assertTrue(draft.options.removeFileName)
            assertEquals("newname", draft.options.customName)
        }
        compose
            .onNode(
                hasContentDescription("Убрать имя файла") and hasAnyAncestor(hasTestTag("attachment-rename-controls")),
            ).performClick()
        compose.runOnIdle {
            assertTrue(!draft.options.removeFileName)
            assertEquals("newname", draft.options.customName)
        }
        compose.onNodeWithContentDescription("Вернуть исходное имя").performClick()
        compose.runOnIdle { assertEquals(null, draft.options.customName) }
        compose.onNodeWithContentDescription("Изменить").performClick()
        compose.runOnIdle { assertEquals(AttachmentEditorTab.CROP, draft.tab) }
    }

    @Test fun otherFileActionsCloseNameWithoutLosingIt() {
        val original = image()
        val draft =
            com.solthariel.pawchan.composer
                .AttachmentViewerDraft(original.options)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(320.dp, 184.dp)) {
                    AttachmentEditorPanel(
                        original.copy(options = draft.currentOptions),
                        draft,
                        com.solthariel.pawchan.composer
                            .ReencodingDimensions(100, 100),
                        true,
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Переименовать").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("kept")
        for (action in listOf("Уникальный хеш", "Удалить метаданные", "Спойлер")) {
            compose.onNodeWithContentDescription(action).performClick()
            compose.runOnIdle {
                assertEquals(AttachmentEditorTab.FILE, draft.tab)
                assertEquals("kept", draft.options.customName)
            }
            compose.onNodeWithText("Имя файла").assertDoesNotExist()
            compose.onNodeWithContentDescription("Переименовать").performClick()
        }
        compose.onNodeWithContentDescription("Перекодировать изображение").performClick()
        compose.onNodeWithText("Настройки перекодирования").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(AttachmentEditorTab.FILE, draft.tab)
            assertEquals("kept", draft.options.customName)
        }
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle {
            assertTrue(draft.options.reencoding != null)
            assertEquals("kept", draft.options.customName)
        }
    }

    private fun image(): ComposerAttachment = ComposerAttachment(ReplyAttachment("image.jpg", "image/jpeg", byteArrayOf(-1, -40)))
}
