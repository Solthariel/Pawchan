package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GallerySelectionBarRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compactBarKeepsDisabledSemanticsAndOnlyRunsEnabledActions() {
        val enabled = mutableStateOf(false)
        var downloads = 0
        var links = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(393.dp)) {
                    GallerySelectionBar(
                        enabled.value,
                        GallerySelectionBarActions({ downloads++ }, { links++ }, null),
                    )
                }
            }
        }
        compose.onNodeWithTag("gallery-selection-bar").assertHeightIsEqualTo(56.dp)
        val download = compose.onNodeWithContentDescription("Скачать")
        val copy = compose.onNodeWithContentDescription("Копировать ссылки")
        download.assertIsNotEnabled().assertHeightIsEqualTo(48.dp)
        copy.assertIsNotEnabled()
        compose.runOnIdle { enabled.value = true }
        download.assertIsEnabled().performClick()
        copy.performClick()
        compose.runOnIdle {
            assertEquals(1, downloads)
            assertEquals(1, links)
        }
    }

    @Test fun fourActionsFitNarrowBarWithFullSizeTouchTargets() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(240.dp)) {
                    GallerySelectionBar(true, GallerySelectionBarActions({}, {}, {}, {}))
                }
            }
        }
        compose.onNodeWithTag("gallery-selection-bar").assertHeightIsEqualTo(56.dp)
        listOf("Скачать", "Копировать ссылки", "В избранное", "Убрать").forEach {
            compose.onNodeWithContentDescription(it).assertWidthIsAtLeast(48.dp).assertHeightIsEqualTo(48.dp)
        }
    }
}
