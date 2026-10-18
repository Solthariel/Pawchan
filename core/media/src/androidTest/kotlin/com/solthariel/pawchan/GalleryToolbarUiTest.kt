package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.GalleryFilter
import com.solthariel.pawchan.media.GalleryToolbar
import com.solthariel.pawchan.media.GalleryToolbarActions
import com.solthariel.pawchan.media.GalleryToolbarState
import com.solthariel.pawchan.media.MediaGalleryContent
import com.solthariel.pawchan.media.MediaGalleryGroup
import org.junit.Rule
import org.junit.Test

class GalleryToolbarUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun filtersAreImmediatelyVisibleAndSwitchWithoutOpeningAMenu() {
        val gallery =
            MediaGalleryContent(
                all = MediaGalleryGroup(99),
                images = MediaGalleryGroup(67),
                animations = MediaGalleryGroup(1),
                videos = MediaGalleryGroup(31),
            )
        compose.setContent {
            var filter by remember { mutableStateOf(GalleryFilter.ALL) }
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.width(360.dp).statusBarsPadding()) {
                    GalleryToolbar(
                        GalleryToolbarState(filter, false, 0),
                        GalleryToolbarActions({}, { filter = it }, {}),
                        gallery,
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Все: 99").assertIsSelected()
        saveMediaChromeScreenshot("gallery-filters")
        for (label in listOf("Изображения: 67", "GIF: 1", "Видео: 31", "Все: 99")) {
            compose.onNodeWithContentDescription(label).performClick().assertIsSelected()
        }
        compose.onNodeWithContentDescription("Фильтр медиа").assertDoesNotExist()
        compose.onNodeWithText("Изображения").assertDoesNotExist()
        compose.onNodeWithText("Видео").assertDoesNotExist()
    }
}
