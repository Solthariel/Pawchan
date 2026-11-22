package com.solthariel.pawchan

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardDirectoryCard
import com.solthariel.pawchan.boards.BoardListActions
import com.solthariel.pawchan.boards.BoardListItem
import com.solthariel.pawchan.boards.BoardsDirectoryList
import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.boards.boardListDimensions
import com.solthariel.pawchan.boards.prepareBoardsList
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardsRowAlignmentRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun categoryDisclosureAlignsWithToolbarOverflowAction() {
        val state =
            BoardsUiState.Content(
                boards = listOf(board("a", "Тестовая")),
                favoriteBoards = emptySet(),
                boardSpeeds = emptyMap(),
                unavailableBoards = emptySet(),
                expandedCategories = setOf("Тематика"),
                isRefreshing = false,
            )
        compose.setContent {
            MaterialTheme {
                BoardsDirectoryList(
                    content = state.prepareBoardsList("", boardListDimensions(), 1f),
                    onExpandedChange = { _, _ -> },
                    actions = BoardListActions({}, { _, _ -> }),
                    modifier = Modifier.width(360.dp).height(220.dp),
                )
            }
        }

        val listRight =
            compose
                .onNodeWithTag("boards-directory-list")
                .fetchSemanticsNode()
                .boundsInRoot.right
        val disclosureCenter =
            compose
                .onNodeWithTag("disclosure-indicator", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot.center.x
        val toolbarActionCenterInset =
            with(compose.density) { (APP_TOOLBAR_HORIZONTAL_INSET + APP_TOOLBAR_BUTTON_SIZE / 2).toPx() }

        assertEquals(listRight - toolbarActionCenterInset, disclosureCenter, 1f)
    }

    @Test
    fun gridShowsThreeEqualCardsAndWrapsLongNamesToTwoLines() {
        val entries =
            listOf(
                board("au", "Автомобили"),
                board("c", "Комиксы и мультипликация"),
                board("cc", "Криптовалюта"),
            )
        val state =
            BoardsUiState.Content(
                boards = entries,
                favoriteBoards = emptySet(),
                boardSpeeds = emptyMap(),
                unavailableBoards = emptySet(),
                expandedCategories = setOf("Тематика"),
                isRefreshing = false,
            )
        compose.setContent {
            MaterialTheme {
                BoardsDirectoryList(
                    content =
                        state.prepareBoardsList("", boardListDimensions(), 1f),
                    onExpandedChange = { _, _ -> },
                    actions = BoardListActions({}, { _, _ -> }),
                    modifier = Modifier.width(360.dp).height(220.dp),
                )
            }
        }

        val cards =
            listOf("au", "c", "cc").map { code ->
                compose.onNodeWithTag("board-card:$code").fetchSemanticsNode().boundsInRoot
            }
        assertTrue(cards[0].right < cards[1].left)
        assertTrue(cards[1].right < cards[2].left)
        assertEquals(cards[0].width, cards[1].width, 0.5f)
        assertEquals(cards[1].width, cards[2].width, 0.5f)

        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText("Комиксы и мультипликация", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        compose.runOnIdle { assertEquals(2, layouts.single().lineCount) }
    }

    @Test
    fun cardSelectionAndFavoriteActionsRemainIndependent() {
        val item =
            BoardListItem.Board(
                entry = board("g", "Технологии", "4chan"),
                favorite = false,
                unavailable = false,
            )
        var selected: BoardAddress? = null
        var favorite: Pair<BoardAddress, Boolean>? = null
        compose.setContent {
            MaterialTheme {
                BoardDirectoryCard(
                    item,
                    BoardListActions({ selected = it }, { address, value -> favorite = address to value }),
                    Modifier.width(118.dp).height(88.dp),
                )
            }
        }

        compose.onNodeWithText("Технологии").performClick()
        compose.runOnIdle {
            assertEquals(item.entry.address, selected)
            selected = null
        }
        compose.onNodeWithContentDescription("Добавить доску в избранное").performClick()
        compose.onNodeWithText("0/ч").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(null, selected)
            assertEquals(item.entry.address to true, favorite)
        }
    }

    private fun board(
        code: String,
        title: String,
        site: String = "dvach",
    ) = BoardDirectoryEntry(
        address =
            BoardAddress(
                requireNotNull(SiteId.parse(site)),
                requireNotNull(BoardId.parse(code)),
            ),
        title = title,
        category = "Тематика",
        postsPerHour = 0,
        postingEnabled = true,
    )
}
