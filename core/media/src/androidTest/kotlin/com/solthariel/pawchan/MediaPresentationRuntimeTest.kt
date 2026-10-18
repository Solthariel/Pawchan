package com.solthariel.pawchan

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.media.LocalMediaPresentation
import com.solthariel.pawchan.media.MediaPresentation
import com.solthariel.pawchan.media.resolvedMediaUrl
import org.junit.Rule
import org.junit.Test

class MediaPresentationRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun standaloneViewerUsesOriginalUrl() {
        compose.setContent { Text(resolvedMediaUrl("https://example.invalid/image")) }
        compose.onNodeWithText("https://example.invalid/image").assertIsDisplayed()
    }

    @Test
    fun hostBindingUsesLocalUrlOnlyInItsComposition() {
        compose.setContent {
            CompositionLocalProvider(LocalMediaPresentation provides MediaPresentation(mapOf("remote" to "local"))) {
                Text(resolvedMediaUrl("remote"))
            }
            Text(resolvedMediaUrl("remote"))
        }
        compose.onNodeWithText("local").assertIsDisplayed()
        compose.onNodeWithText("remote").assertIsDisplayed()
    }

    @Test
    fun emptyLocalBindingFallsBackToOriginalUrl() {
        compose.setContent {
            CompositionLocalProvider(LocalMediaPresentation provides MediaPresentation(mapOf("remote" to ""))) {
                Text(resolvedMediaUrl("remote"))
            }
        }
        compose.onNodeWithText("remote").assertIsDisplayed()
    }
}
