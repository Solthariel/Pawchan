package com.solthariel.pawchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import com.solthariel.pawchan.boards.BoardListActions
import com.solthariel.pawchan.boards.BoardsDirectoryList
import com.solthariel.pawchan.boards.BoardsListUi
import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.boards.boardListDimensions
import com.solthariel.pawchan.boards.prepareBoardsList
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardsScrollbarRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: LazyListState
    private lateinit var content: BoardsListUi
    private var selectedBoard: BoardId? = null
    private var favoriteBoard: BoardId? = null
    private var favoriteValue = false
    private var collapsedCategory: String? = null

    @Test
    fun progressIsMonotonicAndEveryVisibleHeightMatchesTheProfile() {
        showBoards()
        val root = compose.onRoot()
        root.performTouchInput { down(Offset(width - 1f, height * 0.25f)) }
        try {
            compose.waitForIdle()
            var previous = 0f
            for (index in content.rows.indices) {
                scrollTo(index)
                val scrollbar = compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                val progress = scrollbar.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
                assertTrue(progress >= previous)
                previous = progress
                compose.runOnIdle {
                    for (item in state.layoutInfo.visibleItemsInfo) {
                        assertEquals(content.itemSizes.heightAt(item.index), item.size)
                    }
                }
            }
            compose.runOnIdle { assertFalse(state.canScrollForward) }
        } finally {
            root.performTouchInput { up() }
        }
    }

    @Test
    fun largeFontUsesMatchingRowAndPrefixHeights() {
        showBoards(fontScale = 2f)
        for (index in listOf(0, 10, 30, 60, content.rows.lastIndex)) {
            scrollTo(index)
            compose.runOnIdle {
                for (item in state.layoutInfo.visibleItemsInfo) {
                    assertEquals(content.itemSizes.heightAt(item.index), item.size)
                }
            }
        }
    }

    @Test
    fun boardAndFavoriteClicksStillReachTheirOwnActions() {
        showBoards(query = "/board0/")
        compose.onNodeWithText("Доска 0").performClick()
        compose.onNodeWithText("☆").performClick()
        compose.runOnIdle {
            assertEquals("board0", selectedBoard?.value)
            assertEquals("board0", favoriteBoard?.value)
            assertTrue(favoriteValue)
        }
    }

    @Test
    fun categoryHeaderStillCanCollapseItsRows() {
        showBoards()
        compose.onNodeWithText("Пользовательские  71").performClick()
        compose.runOnIdle { assertEquals("Пользовательские", collapsedCategory) }
    }

    private fun showBoards(
        fontScale: Float = 1f,
        query: String = "",
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    BoardsTestContent(query)
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun BoardsTestContent(query: String) {
        val source = remember { sourceState() }
        val dimensions = boardListDimensions()
        val density = LocalDensity.current.density
        content =
            remember(source, dimensions, density, query) {
                source.prepareBoardsList(query, "Избранное", dimensions, density)
            }
        state = rememberLazyListState()
        AppNavigationWorkspace(
            actions = AppBottomActions({}, {}, {}, {}),
            selected = AppBottomDestination.BOARDS,
        ) {
            BoardsDirectoryList(
                content = content,
                onExpandedChange = { key, expanded -> if (!expanded) collapsedCategory = key },
                actions =
                    BoardListActions(
                        onBoardSelected = { selectedBoard = it },
                        onFavoriteChanged = { board, value ->
                            favoriteBoard = board
                            favoriteValue = value
                        },
                    ),
                modifier = Modifier.fillMaxSize(),
                listState = state,
            )
        }
    }

    private fun scrollTo(index: Int) {
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)).performScrollToIndex(index)
        compose.waitForIdle()
    }

    private fun sourceState(): BoardsUiState.Content {
        val boards =
            List(71) { index ->
                BoardDirectoryEntry(
                    address =
                        BoardAddress(
                            site = requireNotNull(SiteId.parse("dvach")),
                            board = requireNotNull(BoardId.parse("board$index")),
                        ),
                    title = "Доска $index",
                    category = "Пользовательские",
                    postsPerHour = index,
                    postingEnabled = true,
                )
            }
        return BoardsUiState.Content(
            boards = boards,
            favoriteBoards = emptySet(),
            boardSpeeds = boards.associate { it.address.board to it.postsPerHour },
            unavailableBoards = emptySet(),
            expandedCategories = setOf("Пользовательские"),
            isRefreshing = false,
        )
    }
}
