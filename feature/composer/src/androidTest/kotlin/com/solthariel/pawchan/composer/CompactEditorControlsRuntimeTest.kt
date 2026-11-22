package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageOptions
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CompactEditorControlsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ratiosFitOneRowAndOrientationDoesNotRotateTheImage() {
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) { ImageAspectRatioControls(draft, ReencodingDimensions(800, 600), true) }
            }
        }
        assertOneRow(listOf("Свободно", "Исходное", "1:1", "4:3", "16:9", "Изменить ориентацию пропорции"))
        compose.onNodeWithContentDescription("4:3").performClick()
        compose.onNodeWithContentDescription("Изменить ориентацию пропорции").performClick()
        compose.onNodeWithContentDescription("3:4").assertIsSelected()
        compose.runOnIdle {
            assertEquals(ImageCropPreset.THREE_FOUR, draft.cropPreset)
            assertEquals(0, draft.editor.edit.quarterTurns)
        }
        compose.onNodeWithContentDescription("1:1").performClick()
        compose.onNodeWithContentDescription("Изменить ориентацию пропорции").assertIsNotEnabled()
    }

    @Test fun allCropPresetsShareOneSelection() {
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        compose.setContent {
            MaterialTheme { ImageAspectRatioControls(draft, ReencodingDimensions(800, 600), true) }
        }
        val presets = listOf("Свободно", "Исходное", "1:1", "4:3", "16:9")
        presets.forEach { selected ->
            compose.onNodeWithContentDescription(selected).performClick().assertIsSelected()
            presets.filterNot { it == selected }.forEach { label ->
                compose.onNodeWithContentDescription(label).assertIsNotSelected()
            }
        }
    }

    @Test fun selectingOriginalOrFreeKeepsTheExistingCrop() {
        val crop = DraftCrop(.1f, .1f, .8f, .5f)
        val draft = AttachmentViewerDraft(MediaDraftOptions(image = DraftImageOptions(edit = DraftImageEdit(crop = crop))))
        compose.setContent {
            MaterialTheme { ImageAspectRatioControls(draft, ReencodingDimensions(800, 600), true) }
        }
        compose.onNodeWithContentDescription("Исходное").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(crop, draft.editor.edit.crop) }
        compose.onNodeWithContentDescription("Свободно").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(crop, draft.editor.edit.crop) }
        compose.onNodeWithContentDescription("1:1").performClick()
        compose.runOnIdle { org.junit.Assert.assertNotEquals(crop, draft.editor.edit.crop) }
    }

    @Test fun sixDrawingToolsFitAndAllColorsRemainAvailable() {
        val selection = ImageToolSelection()
        compose.setContent { MaterialTheme { Box(Modifier.width(320.dp)) { ImageDrawingToolbar(selection, true) } } }
        assertOneRow(listOf("Перо", "Маркер", "Стрелка", "Рамка", "Овал", "Ластик"))
        compose.onNodeWithContentDescription("Стрелка").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(ImageDrawingTool.ARROW, selection.drawing) }
        compose.onNodeWithContentDescription("Выбрать цвет").performClick()
        (1..8).forEach { compose.onNodeWithContentDescription("Цвет $it").assertIsDisplayed() }
        compose.onNodeWithContentDescription("Цвет 8").performClick()
        compose.runOnIdle { assertEquals(IMAGE_EDITOR_COLORS.last(), selection.color) }
    }

    @Test fun redactionChoicesAreIndependentAndSliderDoesNotDisappear() {
        val selection = ImageToolSelection()
        compose.setContent { MaterialTheme { Box(Modifier.width(320.dp)) { ImageRedactionToolbar(selection, true) } } }
        assertOneRow(listOf("Заливка", "Пиксели", "Рамкой", "Кистью"))
        val slider = compose.onNodeWithContentDescription("Размер кисти замазки").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Пиксели").performClick()
        compose.onNodeWithContentDescription("Рамкой").performClick()
        compose.onNodeWithContentDescription("Размер кисти замазки").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(DraftRedactionMode.PIXELS, selection.redactionMode)
            assertEquals(DraftRedactionTool.RECTANGLE, selection.redactionTool)
        }
        assertEquals(slider, compose.onNodeWithContentDescription("Размер кисти замазки").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Кистью").performClick()
        compose.onNodeWithContentDescription("Пиксели").assertIsSelected()
    }

    @Test fun rotationDrawingAndRedactionButtonsShareOneVisibleHeight() {
        val draft = AttachmentViewerDraft(MediaDraftOptions())
        val selection = ImageToolSelection()
        compose.setContent {
            MaterialTheme {
                Column {
                    ImageRotationControls(draft, true)
                    ImageDrawingToolbar(selection, true)
                    ImageRedactionToolbar(selection, true)
                }
            }
        }
        val labels =
            listOf(
                "Повернуть влево на 90°",
                "Повернуть вправо на 90°",
                "Отразить по горизонтали",
                "Отразить по вертикали",
                "Перо",
                "Маркер",
                "Стрелка",
                "Рамка",
                "Овал",
                "Ластик",
                "Заливка",
                "Пиксели",
                "Рамкой",
                "Кистью",
            )
        val heights =
            labels.map { label ->
                compose
                    .onNodeWithContentDescription(label)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot.height
            }
        heights.forEach { assertEquals(48f * density(), it, 1f) }
    }

    private fun assertOneRow(labels: List<String>) {
        val bounds =
            labels.map {
                compose
                    .onNodeWithContentDescription(it)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot
            }
        bounds.forEach { assertEquals(bounds.first().top, it.top, .5f) }
        bounds.zipWithNext().forEach { (a, b) -> org.junit.Assert.assertTrue(a.right <= b.left + .5f) }
    }

    private fun density(): Float =
        androidx.test.platform.app.InstrumentationRegistry
            .getInstrumentation()
            .targetContext.resources.displayMetrics.density
}
