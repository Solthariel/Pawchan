package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
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

    @Test fun editsPersistImmediatelyAndBackKeepsThemWhenReopened() {
        val saved = mutableStateOf(ComposerAttachment(ReplyAttachment("test.jpg", "image/jpeg", byteArrayOf(-1, -40))))
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
        compose.onNodeWithContentDescription("Уникальный хеш").performClick()
        compose.onNodeWithContentDescription("Удалить метаданные").performClick()
        compose.onNodeWithText("Применить").assertDoesNotExist()
        compose.onNodeWithContentDescription("Переименовать").performClick()
        compose.onNodeWithText("Переименовать").performTextReplacement("renamed")
        Espresso.pressBack()
        compose.runOnIdle {
            assertTrue(saved.value.options.uniqueHash)
            assertTrue(saved.value.options.removeMetadata)
        }
        Espresso.pressBack()
        compose.onNodeWithText("Открыть вложение").performClick()
        compose.onNodeWithContentDescription("Переименовать").performClick()
        compose.onNodeWithText("renamed").assertExists()
        Espresso.pressBack()
        compose.onNodeWithContentDescription("Уникальный хеш").assertIsOn()
        compose.onNodeWithContentDescription("Удалить метаданные").assertIsOn()
        compose.onNodeWithContentDescription("Спойлер").performClick()
        Espresso.pressBack()
        compose.runOnIdle {
            assertTrue(saved.value.options.spoiler)
            assertEquals("renamed", saved.value.options.customName)
        }
        compose.onNodeWithText("Открыть вложение").performClick()
        compose.onNodeWithContentDescription("Спойлер").assertIsOn()
        compose.onNodeWithContentDescription("Уникальный хеш").assertIsOn()
    }
}
