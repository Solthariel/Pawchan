package com.solthariel.pawchan

import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardListDimensions
import com.solthariel.pawchan.boards.BoardListItem
import com.solthariel.pawchan.boards.BoardsUiState
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
    fun expandedCategoryGroupsBoardsIntoThreeColumnRows() {
        val content = state().prepareBoardsList("", dimensions, 1f)
        val gridRows = content.rows.filterIsInstance<BoardListItem.GridRow>()

        assertEquals(25, content.rows.size)
        assertEquals(24, gridRows.size)
        assertEquals(71, gridRows.sumOf { it.boards.size })
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
        val content = state().copy(expandedCategories = emptySet()).prepareBoardsList("", dimensions, 1f)
        assertEquals(1, content.rows.size)
        assertTrue(content.rows.single() is BoardListItem.Header)
    }

    @Test
    fun favoritesFilterShowsOnlyFavoritedBoardsWithoutASeparateSection() {
        val source = state()
        val favorite = source.boards.first().address
        val content =
            source
                .copy(favoriteBoards = setOf(favorite))
                .prepareBoardsList("", dimensions, 1f, favoritesOnly = true)
        val visibleBoards = content.rows.filterIsInstance<BoardListItem.GridRow>().flatMap { it.boards }

        assertEquals(1, visibleBoards.size)
        assertEquals(favorite, visibleBoards.single().entry.address)
        assertTrue(visibleBoards.single().favorite)
        assertFalse(content.rows.any { it is BoardListItem.Header && it.categoryKey == "__favorites__" })
    }

    @Test
    fun searchAutomaticallyExpandsOnlyMatchingRows() {
        val content =
            state()
                .copy(expandedCategories = emptySet())
                .prepareBoardsList("/board20/", dimensions, 1f)
        assertEquals(2, content.rows.size)
        val header = content.rows.first() as BoardListItem.Header
        val gridRow = content.rows.last() as BoardListItem.GridRow
        assertTrue(header.expanded)
        assertFalse(header.enabled)
        assertEquals("/board20/", gridRow.boards.single().codeLabel)
    }

    @Test
    fun emptyFavoritesAndSearchHaveDifferentEmptyStates() {
        val source = state()
        val favorites = source.prepareBoardsList("", dimensions, 1f, favoritesOnly = true)
        val search = source.prepareBoardsList("no matches", dimensions, 1f)

        assertEquals(BoardListItem.Empty(favoritesOnly = true), favorites.rows.single())
        assertEquals(BoardListItem.Empty(favoritesOnly = false), search.rows.single())
    }

    @Test
    fun pixelProfileIncludesCategoryAndGridRowGaps() {
        val content = state().prepareBoardsList("", dimensions, 1.5f)

        assertEquals(60, content.itemSizes.heightAt(0))
        assertEquals(114, content.itemSizes.heightAt(1))
        assertEquals(114, content.itemSizes.heightAt(2))
        assertEquals(114, content.itemSizes.heightAt(3))
    }

    @Test
    fun categoryChipsShowAllOrOnlySelectedCategoryEvenWhenCollapsed() {
        val source = state()
        val second = source.boards.first().copy(category = "Творчество")
        val multi = source.copy(boards = source.boards.drop(1) + second, expandedCategories = emptySet())
        val all = multi.prepareBoardsList("", dimensions, 1f, categoryFilter = "")
        val chosen = multi.prepareBoardsList("", dimensions, 1f, categoryFilter = "Творчество")
        val missing = multi.prepareBoardsList("", dimensions, 1f, categoryFilter = "Нет")
        assertEquals(71, all.rows.filterIsInstance<BoardListItem.GridRow>().sumOf { it.boards.size })
        assertFalse(all.rows.any { it is BoardListItem.Header })
        assertEquals(
            listOf(second.address),
            chosen.rows
                .filterIsInstance<BoardListItem.GridRow>()
                .flatMap { it.boards }
                .map { it.entry.address },
        )
        assertTrue(missing.rows.single() is BoardListItem.Empty)
    }

    @Test
    fun categoryFilterCombinesWithSearchAndFavorites() {
        val source = state()
        val target = source.boards.first().copy(category = "Творчество")
        val multi = source.copy(boards = source.boards.drop(1) + target, favoriteBoards = setOf(target.address))
        val chosen =
            multi.prepareBoardsList("board0", dimensions, 1f, favoritesOnly = true, categoryFilter = "Творчество")
        assertEquals(
            target.address,
            chosen.rows
                .filterIsInstance<BoardListItem.GridRow>()
                .single()
                .boards
                .single()
                .entry.address,
        )
        val wrong =
            multi.prepareBoardsList(
                "board0",
                dimensions,
                1f,
                favoritesOnly = true,
                categoryFilter = "Пользовательские",
            )
        assertTrue(wrong.rows.single() is BoardListItem.Empty)
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
            boardSpeeds = boards.associate { it.address to it.postsPerHour },
            unavailableBoards = emptySet(),
            expandedCategories = setOf("Пользовательские"),
            isRefreshing = false,
        )
    }
}
