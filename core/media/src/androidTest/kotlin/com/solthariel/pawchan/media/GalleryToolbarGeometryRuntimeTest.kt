package com.solthariel.pawchan.media

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GalleryToolbarGeometryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filtersStayOnTheHeaderRowAndDoNotOverlapTheSelectionAction() {
        var selectedFilter = GalleryFilter.VIDEO
        var selections = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(393.dp)) {
                    GalleryToolbar(
                        GalleryToolbarState(GalleryFilter.ALL, false, 0),
                        GalleryToolbarActions({}, { selectedFilter = it }, { selections++ }),
                        MediaGalleryContent(all = MediaGalleryGroup(count = 19)),
                    )
                }
            }
        }
        val back = compose.onNodeWithContentDescription("Назад").fetchSemanticsNode().boundsInRoot
        val all = compose.onNodeWithContentDescription("Все: 19")
        val select = compose.onNodeWithContentDescription("Выделить медиа")
        all.assertIsDisplayed().performClick()
        assertEquals(
            back.center.y,
            all
                .fetchSemanticsNode()
                .boundsInRoot.center.y,
            1f,
        )
        assertTrue(all.fetchSemanticsNode().boundsInRoot.right <= select.fetchSemanticsNode().boundsInRoot.left)
        select.performClick()
        compose.runOnIdle {
            assertEquals(GalleryFilter.ALL, selectedFilter)
            assertEquals(1, selections)
        }
    }

    @Test fun enteringSelectionKeepsTheSameHeaderHeight() {
        val selecting = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(393.dp)) {
                    GalleryToolbar(
                        GalleryToolbarState(GalleryFilter.ALL, selecting.value, 0),
                        GalleryToolbarActions({}, {}, { selecting.value = !selecting.value }),
                        MediaGalleryContent(),
                    )
                }
            }
        }
        compose.onNodeWithTag("gallery-toolbar").assertHeightIsEqualTo(64.dp)
        compose.onNodeWithContentDescription("Выделить медиа").performClick()
        compose.onNodeWithTag("gallery-toolbar").assertHeightIsEqualTo(64.dp)
        compose.onNodeWithText("Выбрано: 0").assertIsDisplayed()
    }

    @Test fun narrowGalleryCanScrollToEveryFilterWithoutMovingBackOrSelection() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    GalleryToolbar(
                        GalleryToolbarState(GalleryFilter.ALL, false, 0),
                        GalleryToolbarActions({}, {}, {}),
                        MediaGalleryContent(all = MediaGalleryGroup(count = 9999)),
                    )
                }
            }
        }
        val select = compose.onNodeWithContentDescription("Выделить медиа").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Видео: 0").performScrollTo().assertIsDisplayed()
        val after = compose.onNodeWithContentDescription("Выделить медиа").fetchSemanticsNode().boundsInRoot
        assertEquals(select, after)
        compose.onNodeWithContentDescription("Назад").assertIsDisplayed()
    }
}
