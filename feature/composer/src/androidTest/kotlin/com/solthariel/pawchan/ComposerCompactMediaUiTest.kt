package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import com.solthariel.pawchan.composer.AttachmentOptionsPanel
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerCompactMediaUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allSixActionsStayOnOneRowAtPhoneWidth() {
        assertOptionsAtWidth(304.dp)
    }

    @Test fun allSixCaptionsFitNarrowPhoneWidth() {
        assertOptionsAtWidth(280.dp)
    }

    private fun assertOptionsAtWidth(width: Dp) {
        val file = ComposerAttachment(ReplyAttachment("image.jpg", "image/jpeg", byteArrayOf(-1, -40)))
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(width)) {
                    AttachmentOptionsPanel(file, true, Modifier.testTag("options")) {}
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
            labels.map { label ->
                compose
                    .onNodeWithContentDescription(label)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot
            }
        bounds.forEach { rect ->
            assertTrue(rect.width > 0f)
            assertTrue(rect.left >= panel.left - 0.5f)
            assertTrue(rect.right <= panel.right + 0.5f)
            assertEquals(bounds.first().top, rect.top, 0.5f)
        }
        bounds.zipWithNext().forEach { (left, right) -> assertTrue(left.right <= right.left + 0.5f) }
        val captions = listOf("Изменить\nхеш", "Удалить\nметаданные", "Убрать\nимя", "Спойлер", "Перекоди-\nровать", "Имя\nфайла")
        val captionBounds =
            captions.map { caption ->
                val layouts = mutableListOf<TextLayoutResult>()
                compose.onNodeWithText(caption).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                    it(layouts)
                }
                assertTrue(layouts.isNotEmpty())
                assertTrue(layouts.none { it.hasVisualOverflow })
                compose
                    .onNodeWithText(caption)
                    .assertIsDisplayed()
                    .fetchSemanticsNode()
                    .boundsInRoot
            }
        captionBounds.zip(bounds).forEach { (caption, button) ->
            assertTrue(caption.top >= button.bottom - 0.5f)
            assertEquals(button.center.x, caption.center.x, 0.5f)
            assertEquals(captionBounds.first().top, caption.top, 0.5f)
        }
    }

    @Test fun scaleChipsApplyOnlyAfterApply() {
        val file = mutableStateOf(ComposerAttachment(ReplyAttachment("image.jpg", "image/jpeg", byteArrayOf(-1, -40))))
        compose.setContent {
            MaterialTheme { AttachmentOptionsPanel(file.value, true) { file.value = it } }
        }
        compose.onNodeWithContentDescription("Перекодировать изображение").performClick()
        compose.onNodeWithText("8×").performClick()
        compose.runOnIdle { assertNull(file.value.options.reencoding) }
        Espresso.pressBack()
        compose.runOnIdle { assertNull(file.value.options.reencoding) }
        compose.onNodeWithContentDescription("Перекодировать изображение").performClick()
        compose.onNodeWithText("8×").performClick()
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle {
            val settings = requireNotNull(file.value.options.reencoding)
            assertEquals(8, settings.scaleDivisor)
            assertEquals(0.125f, settings.scaleFactor, 0f)
        }
    }
}
