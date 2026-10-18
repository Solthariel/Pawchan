package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentDisplay
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaToolbar
import com.solthariel.pawchan.media.MediaViewerNavigation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaToolbarRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun toolbarDisplaysFileMetadataAndRoutesActions() {
        var closed = 0
        var saved = 0
        var refreshed = 0
        val item =
            MediaAttachmentUi(
                MediaAttachmentSource("1", "IMG7351.webp", "https://example.com/1.webp", null),
                null,
                AttachmentViewer.IMAGE,
                display = MediaAttachmentDisplay("WEBP", "262 kB", "1332×1920"),
            )
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MediaToolbar(item, MediaViewerNavigation(total = 4), { closed++ }, { saved++ }, { refreshed++ })
            }
        }
        compose.onNodeWithText("IMG7351.webp").assertIsDisplayed()
        compose.onNodeWithText("1/4, 1332×1920, 262 kB").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithContentDescription("Сохранить").performClick()
        compose.onNodeWithContentDescription("Обновить файл").performClick()
        assertEquals(1, closed)
        assertEquals(1, saved)
        assertEquals(1, refreshed)
    }
}
