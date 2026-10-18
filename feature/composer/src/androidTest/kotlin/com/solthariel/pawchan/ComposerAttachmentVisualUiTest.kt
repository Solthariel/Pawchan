package com.solthariel.pawchan

import android.util.Base64
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerAttachmentViewer
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Rule
import org.junit.Test

class ComposerAttachmentVisualUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun attachmentEditorHasSummaryAndRemoveWithoutConfirmation() {
        val bytes = Base64.decode(PREVIEW_PNG, Base64.DEFAULT)
        val file = ComposerAttachment(ReplyAttachment("пример.png", "image/png", bytes), dimensions = "1×1")
        compose.setContent {
            val context = LocalContext.current
            val loader = remember { ImageLoader(context) }
            DisposableEffect(loader) { onDispose { loader.shutdown() } }
            MaterialTheme(colorScheme = darkColorScheme()) {
                ComposerAttachmentViewer(file, loader, true, {}, {}, {})
            }
        }
        compose.onNodeWithText("Спойлер").assertIsDisplayed()
        compose.onNodeWithText("Переименовать").assertIsDisplayed()
        compose.onNodeWithText("Удалить вложение").assertIsDisplayed()
        compose.onNodeWithText("Применить").assertDoesNotExist()
        compose.onNodeWithText("ОК").assertDoesNotExist()
        compose.onNodeWithText("Отмена").assertDoesNotExist()
        saveComposerChromeScreenshot("attachment-editor-compact-autosave")
    }
}

private const val PREVIEW_PNG =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aHYsAAAAASUVORK5CYII="
