package com.solthariel.pawchan

import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardGroupPosition
import com.solthariel.pawchan.boards.BoardListDimensions
import com.solthariel.pawchan.boards.BoardListItem
import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.boards.FAVORITES_CATEGORY_KEY
import com.solthariel.pawchan.boards.prepareBoardsList
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardsListUiTest {
    private val dimensions = BoardListDimensions(40.dp, 68.dp, 72.dp)

    @Test
    fun expandedCategoryHasOneLazyItemForEachBoard() {
        val content = state().prepareBoardsList("", "Избранное", dimensions, 1f)
        assertEquals(73, content.rows.size)
        assertEquals(71, content.rows.filterIsInstance<BoardListItem.Board>().size)
        assertEquals(content.rows.size, content.itemSizes.itemCount)
        assertEquals(
            content.rows.size,
            content.rows
                .map { it.key }
                .toSet()
                .size,
        )
    }

    @Test
    fun collapsedCategoryDoesNotKeepItsBoardsComposed() {
        val content = state().copy(expandedCategories = emptySet()).prepareBoardsList("", "Избранное", dimensions, 1f)
        assertEquals(2, content.rows.size)
        assertTrue(content.rows.all { it is BoardListItem.Header })
    }

    @Test
    fun favoritesAndDirectoryUseDistinctStableKeys() {
        val state = state()
        val content =
            state
                .copy(
                    favoriteBoards =
                        setOf(
                            state.boards
                                .first()
                                .address.board,
                        ),
                    expandedCategories = state.expandedCategories + FAVORITES_CATEGORY_KEY,
                ).prepareBoardsList("", "Избранное", dimensions, 1f)
        val firstBoard =
            content.rows
                .filterIsInstance<BoardListItem.Board>()
                .filter { it.entry == state.boards.first() }
        assertEquals(2, firstBoard.size)
        assertEquals(2, firstBoard.map { it.key }.toSet().size)
        assertTrue(firstBoard.all { it.favorite })
    }

    @Test
    fun searchAutomaticallyExpandsOnlyMatchingRows() {
        val content =
            state()
                .copy(expandedCategories = emptySet())
                .prepareBoardsList("/board20/", "Избранное", dimensions, 1f)
        assertEquals(2, content.rows.size)
        val header = content.rows.first() as BoardListItem.Header
        assertTrue(header.expanded)
        assertFalse(header.enabled)
        assertEquals("/board20/", (content.rows.last() as BoardListItem.Board).codeLabel)
    }

    @Test
    fun emptySearchHasAnEmptyRowAndSafePixelSizes() {
        val content = state().prepareBoardsList("нет совпадений", "Избранное", dimensions, 1f)
        assertEquals(listOf(BoardListItem.Empty), content.rows)
        assertEquals(72L, content.itemSizes.totalHeight)
    }

    @Test
    fun pixelProfileIncludesCategoryGapsAndRowSeparators() {
        val content = state().prepareBoardsList("", "Избранное", dimensions, 1.5f)
        assertEquals(60, content.itemSizes.heightAt(0))
        assertEquals(87, content.itemSizes.heightAt(1))
        assertEquals(114, content.itemSizes.heightAt(2))
        assertEquals(104, content.itemSizes.heightAt(3))
        val rows = content.rows.filterIsInstance<BoardListItem.Board>()
        assertEquals(BoardGroupPosition.FIRST, rows.first().position)
        assertEquals(BoardGroupPosition.LAST, rows.last().position)
    }

    private fun state(): BoardsUiState.Content {
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
