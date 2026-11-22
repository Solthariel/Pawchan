package com.solthariel.pawchan

import android.util.Base64
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.espresso.Espresso
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerAttachmentViewer
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttachmentApplyReopenUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toolbarAndSystemBackApplyOptionsWithoutDoneOrDiscardPrompt() {
        val bytes = Base64.decode(PREVIEW_PNG, Base64.DEFAULT)
        val saved = mutableStateOf(ComposerAttachment(ReplyAttachment("test.png", "image/png", bytes)))
        val opened = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                TextButton(onClick = { opened.value = true }) { Text("Открыть вложение") }
                if (opened.value) {
                    val context = LocalContext.current
                    val loader = androidx.compose.runtime.remember(context) { ImageLoader.Builder(context).build() }
                    androidx.compose.runtime.DisposableEffect(loader) { onDispose { loader.shutdown() } }
                    ComposerAttachmentViewer(
                        saved.value,
                        loader,
                        true,
                        { opened.value = false },
                        { saved.value = it },
                        { opened.value = false },
                    )
                }
            }
        }
        awaitEditor()
        compose.onNodeWithContentDescription("Уникальный хеш", useUnmergedTree = true).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Удалить метаданные", useUnmergedTree = true).performClick()
        compose.onNodeWithContentDescription("Переименовать", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Имя файла").performTextReplacement("renamed")
        compose.onNodeWithText("Сохранить").assertDoesNotExist()
        compose.onNodeWithText("Готово").assertDoesNotExist()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.waitUntil(10_000) { !opened.value }
        compose.runOnIdle {
            assertTrue(saved.value.options.uniqueHash)
            assertTrue(saved.value.options.removeMetadata)
            assertEquals("renamed", saved.value.options.customName)
        }
        compose.onNodeWithText("Открыть вложение").performClick()
        awaitEditor()
        compose.onNodeWithContentDescription("Уникальный хеш", useUnmergedTree = true).assertIsOn()
        compose.onNodeWithContentDescription("Удалить метаданные", useUnmergedTree = true).assertIsOn()
        compose.onNodeWithContentDescription("Спойлер", useUnmergedTree = true).performClick()
        Espresso.pressBack()
        compose.waitUntil(10_000) { !opened.value }
        compose.onNodeWithText("Отменить изменения?").assertDoesNotExist()
        compose.runOnIdle { assertTrue(saved.value.options.spoiler) }
        compose.onNodeWithText("Открыть вложение").performClick()
        awaitEditor()
        compose.onNodeWithContentDescription("Спойлер", useUnmergedTree = true).assertIsOn()
    }

    private fun awaitEditor() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Уникальный хеш", useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
    }
}

private const val PREVIEW_PNG =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aHYsAAAAASUVORK5CYII="
