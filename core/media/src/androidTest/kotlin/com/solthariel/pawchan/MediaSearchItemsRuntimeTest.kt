package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.ImageSearchEngine
import com.solthariel.pawchan.media.MediaMenuActions
import com.solthariel.pawchan.media.MediaSearchItems
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MediaSearchItemsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bothProvidersReserveTheStandardLeadingIconAndKeepTheirActions() {
        val selected = mutableListOf<ImageSearchEngine>()
        var back = 0
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(240.dp)) {
                    MediaSearchItems(MediaMenuActions({}, {}, null, { selected += it }, {})) { back++ }
                }
            }
        }
        val google = compose.onNodeWithText("Google", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val yandex = compose.onNodeWithText("Яндекс", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(google.left, yandex.left, 1f)
        assertTrue(google.left >= 48f * compose.density.density)
        compose.onNodeWithText("Google").performClick()
        compose.onNodeWithText("Яндекс").performClick()
        compose.onNodeWithText("Назад").performClick()
        assertEquals(listOf(ImageSearchEngine.GOOGLE, ImageSearchEngine.YANDEX), selected)
        assertEquals(1, back)
    }
}
