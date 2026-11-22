package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Regression: finding a semantic node alone does not prove that a weighted button has any visible width. */
class EditorButtonsVisibilityRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allSixFileActionsHaveSeparateVisibleCellsOnANarrowScreen() {
        val file = ComposerAttachment(ReplyAttachment("image.png", "image/png", byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)))
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    AttachmentOptionsPanel(file, true, Modifier.testTag("file-actions"), onEditorAction = {}) {}
                }
            }
        }
        assertSeparateCells(
            "file-actions",
            listOf("Изменить", "Уникальный хеш", "Удалить метаданные", "Спойлер", "Перекодировать изображение", "Переименовать"),
        )
    }

    @Test fun allSixEditorActionsRemainVisibleTogether() {
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) { AttachmentEditorTabs(draft, true) }
            }
        }
        assertSeparateCells(
            "editor-tool-tabs",
            listOf("Назад к файлу", "Пропорции", "Поворот", "Рисовать", "Замазать", "Сравнить"),
        )
    }

    @Test fun weightedToolChoicesDoNotCollapseToTheFirstButton() {
        val labels = listOf("Первый", "Второй", "Третий", "Четвёртый")
        compose.setContent {
            MaterialTheme {
                Row(Modifier.width(320.dp).testTag("tool-choices")) {
                    labels.forEach { label ->
                        EditorChoiceButton(EditorChoice(label, caption = label), true, Modifier.weight(1f)) {}
                    }
                }
            }
        }
        assertSeparateCells("tool-choices", labels)
    }

    private fun assertSeparateCells(
        rowTag: String,
        labels: List<String>,
    ) {
        val row = compose.onNodeWithTag(rowTag).fetchSemanticsNode().boundsInRoot
        val cells =
            labels.map { label ->
                val node = compose.onNodeWithContentDescription(label).assertIsDisplayed().assertHasClickAction()
                node.fetchSemanticsNode().boundsInRoot.also { bounds ->
                    assertTrue("$label has no usable width", bounds.width >= row.width / labels.size * .9f)
                    assertTrue("$label is outside its row", bounds.left >= row.left && bounds.right <= row.right + 1f)
                }
            }
        cells.zipWithNext().forEach { (left, right) ->
            assertTrue("Buttons overlap", left.right <= right.left + 1f)
        }
    }
}
