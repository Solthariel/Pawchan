package com.solthariel.pawchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.boards.BoardSourcePicker
import com.solthariel.pawchan.boards.BoardsFavoritesFilter
import com.solthariel.pawchan.boards.BoardsSearchBarActions
import com.solthariel.pawchan.boards.BoardsSearchBarState
import com.solthariel.pawchan.boards.BoardsSearchField
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource
import com.solthariel.pawchan.infrastructure.sites.ForumCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardSourcePickerRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectionKeepsSourceIdentityAndUsesPlainLabels() {
        val first = requireNotNull(SiteId.parse("dvach"))
        val second = requireNotNull(SiteId.parse("4chan"))
        val directory =
            object : BoardDirectoryRepository {
                override suspend fun loadBoards(forceRefresh: Boolean) =
                    DomainOutcome.Success(emptyList<com.solthariel.pawchan.domain.boards.BoardDirectoryEntry>())
            }
        val sources =
            listOf(
                ForumBoardSource(first, "2ch", directory, ForumCapabilities(replies = true), R.drawable.forum_2ch),
                ForumBoardSource(second, "4chan", directory, icon = R.drawable.forum_4chan),
            )
        var chosen = first
        val selected = mutableStateOf(first)
        compose.setContent {
            MaterialTheme(
                colorScheme =
                    darkColorScheme(
                        primary = Color.Magenta,
                        onSurfaceVariant = Color.Cyan,
                        secondaryContainer = Color.Blue,
                    ),
            ) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    BoardSourcePicker(sources, selected.value) {
                        chosen = it
                        selected.value = it
                    }
                }
            }
        }
        compose.onNodeWithTag("board-source-panel").assertExists()
        val firstBounds = compose.onNodeWithText("2ch").fetchSemanticsNode().boundsInRoot
        val secondBounds = compose.onNodeWithText("4chan").fetchSemanticsNode().boundsInRoot
        assertTrue(secondBounds.left > firstBounds.left)
        compose.onNodeWithText("2ch").assertIsSelected()
        compose.onNodeWithText("4chan").assertExists()
        compose.runOnIdle { selected.value = second }
        compose.onNodeWithText("4chan").assertIsSelected()
        compose.onNodeWithText("2ch").assertIsNotSelected()
        compose.runOnIdle { selected.value = first }
        compose.onNodeWithText("4chan").performClick().assertIsSelected()
        compose.onNodeWithText("2ch").assertIsNotSelected()
        compose.runOnIdle { assertEquals(second, chosen) }
    }

    @Test fun selectedSourceRemainsVisibleWhenThemeColorsCoincide() {
        val first = requireNotNull(SiteId.parse("dvach"))
        val second = requireNotNull(SiteId.parse("4chan"))
        val directory =
            object : BoardDirectoryRepository {
                override suspend fun loadBoards(forceRefresh: Boolean) =
                    DomainOutcome.Success(emptyList<com.solthariel.pawchan.domain.boards.BoardDirectoryEntry>())
            }
        val sources =
            listOf(
                ForumBoardSource(first, "2ch", directory, ForumCapabilities(replies = true), R.drawable.forum_2ch),
                ForumBoardSource(second, "4chan", directory, icon = R.drawable.forum_4chan),
            )
        val selected = mutableStateOf(first)
        compose.setContent {
            MaterialTheme(
                colorScheme =
                    darkColorScheme(primary = Color.White, outline = Color.White, onSurfaceVariant = Color.White),
            ) {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    BoardSourcePicker(sources, selected.value) { selected.value = it }
                }
            }
        }
        val firstNode = compose.onNodeWithText("2ch")
        val selectedPixels = firstNode.assertIsSelected().captureToImage().toPixelMap()
        compose.runOnIdle { selected.value = second }
        val unselectedPixels = firstNode.assertIsNotSelected().captureToImage().toPixelMap()
        assertEquals(selectedPixels.width, unselectedPixels.width)
        assertEquals(selectedPixels.height, unselectedPixels.height)
        val density =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext.resources.displayMetrics.density
        val borderY = (1.5f * density).toInt().coerceAtLeast(1)
        assertNotEquals(
            selectedPixels[selectedPixels.width / 2, borderY],
            unselectedPixels[unselectedPixels.width / 2, borderY],
        )
    }

    @Test fun favoritesFilterHasClearSelectedState() {
        compose.setContent {
            var selected by remember { mutableStateOf(false) }
            MaterialTheme {
                BoardsFavoritesFilter(selected, { selected = !selected })
            }
        }

        compose
            .onNodeWithTag("boards-favorites-filter")
            .assertIsNotSelected()
        compose.onNodeWithTag("boards-favorites-filter").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Показать все доски").assertExists()
    }

    @Test fun favoritesFilterPrecedesOverflowAndRefreshIsInMenu() {
        var refreshes = 0
        compose.setContent {
            var selected by remember { mutableStateOf(false) }
            MaterialTheme {
                BoardsSearchField(
                    BoardsSearchBarState("", false, false),
                    BoardsSearchBarActions({}, {}, {}, { refreshes++ }, { selected = !selected }),
                    selected,
                )
            }
        }
        val overflow = compose.onNodeWithTag("boards-overflow").fetchSemanticsNode().boundsInRoot
        val filter = compose.onNodeWithTag("boards-favorites-filter").fetchSemanticsNode().boundsInRoot
        assertTrue(filter.right <= overflow.left + .5f)
        assertEquals(overflow.width, filter.width, .5f)
        compose.onNodeWithTag("boards-favorites-filter").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Меню досок").performClick()
        compose.onNodeWithText("Обновить").performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }
}
