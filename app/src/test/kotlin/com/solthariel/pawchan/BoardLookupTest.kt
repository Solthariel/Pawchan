package com.solthariel.pawchan

import com.solthariel.pawchan.boards.BOARD_LOOKUP_COLUMNS
import com.solthariel.pawchan.boards.BoardLookupSource
import com.solthariel.pawchan.boards.boardLookupRowBudget
import com.solthariel.pawchan.boards.prepareLookup
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardLookupTest {
    @Test
    fun favoritesAndRecentNeverDuplicateEvenIfDirectoryIsUnavailable() {
        val source =
            BoardLookupSource(
                favorites = linkedSetOf(board("b"), board("mobi")),
                recent = listOf(board("b"), board("po"), board("po"), board("other", "another")),
            )
        val result = source.prepareLookup(board("b"), "")
        assertEquals(listOf("/b/", "/mobi/"), result.favorites.map { it.code })
        assertEquals(listOf("/po/"), result.recent.map { it.code })
        assertTrue(result.favorites.first().selected)
    }

    @Test
    fun favoritesAreFilteredByFullSiteAddress() {
        val source = BoardLookupSource(favorites = setOf(board("g"), board("g", "another")))
        assertEquals(listOf(board("g")), source.prepareLookup(board("b"), "").favorites.map { it.address })
        assertEquals(
            listOf(board("g", "another")),
            source.prepareLookup(board("b", "another"), "").favorites.map { it.address },
        )
    }

    @Test
    fun selectedForumControlsSearchAndQuickListsWithoutChangingTheOpenBoard() {
        val selected = requireNotNull(SiteId.parse("4chan"))
        val source =
            BoardLookupSource(
                boards = listOf(entry("b", "Двач", "test"), entry("b", "Random", "4chan")),
                favorites = setOf(board("b"), board("b", "4chan")),
                recent = listOf(board("a", "4chan"), board("a")),
            )
        val results = source.prepareLookup(board("b"), "random", selected)
        assertEquals(listOf(board("b", "4chan")), results.suggestions.map { it.address })
        assertTrue(results.suggestions.none { it.selected })
        val quick = source.prepareLookup(board("b"), "", selected)
        assertEquals(listOf(board("b", "4chan")), quick.favorites.map { it.address })
        assertEquals(listOf(board("a", "4chan")), quick.recent.map { it.address })
    }

    @Test
    fun searchRanksExactCodeBeforePrefixesAndTitles() {
        val source =
            BoardLookupSource(
                boards = listOf(entry("mobile", "Телефоны"), entry("mobi", "Устройства"), entry("a", "Mobi forum")),
            )
        val result = source.prepareLookup(board("b"), " /MOBI/ ")
        assertEquals(listOf("/mobi/", "/mobile/", "/a/"), result.suggestions.map { it.code })
    }

    @Test
    fun russianTitleSearchIsCaseInsensitiveAndExcludesOtherSites() {
        val source =
            BoardLookupSource(
                boards = listOf(entry("mobi", "Мобильные устройства"), entry("b", "Бред", "another")),
            )
        assertEquals(listOf("/mobi/"), source.prepareLookup(board("b"), "мобиль").suggestions.map { it.code })
        assertTrue(source.prepareLookup(board("b"), "Бред").suggestions.isEmpty())
    }

    @Test
    fun clearingSearchRestoresQuickGroups() {
        val source = BoardLookupSource(boards = listOf(entry("b", "Бред")), favorites = setOf(board("b")))
        assertEquals(1, source.prepareLookup(board("b"), "б").suggestions.size)
        assertTrue(source.prepareLookup(board("b"), "").suggestions.isEmpty())
        assertEquals(1, source.prepareLookup(board("b"), "").favorites.size)
    }

    @Test
    fun eachGroupIsCappedAtTwoRowsWithoutRedistribution() {
        for (favoriteRows in 0..12) {
            for (recentRows in 0..12) {
                val result = boardLookupRowBudget(favoriteRows, recentRows)
                assertEquals(minOf(2, favoriteRows) + minOf(2, recentRows), result.favorites + result.recent)
                assertTrue(result.favorites in 0..favoriteRows)
                assertTrue(result.recent in 0..recentRows)
            }
        }
        assertEquals(2, boardLookupRowBudget(10, 10).favorites)
        assertEquals(2, boardLookupRowBudget(1, 10).recent)
        assertEquals(2, boardLookupRowBudget(10, 1).favorites)
        assertEquals(2, boardLookupRowBudget(0, 10).recent)
    }

    @Test
    fun fiveColumnsAreFixedAndSpareRowsAreNeverTransferred() {
        assertEquals(5, BOARD_LOOKUP_COLUMNS)
        assertEquals(2, boardLookupRowBudget(20, 0).favorites)
        assertEquals(0, boardLookupRowBudget(20, 0).recent)
        assertEquals(1, boardLookupRowBudget(20, 1).recent)
        assertEquals(2, boardLookupRowBudget(20, 1).favorites)
    }

    private fun board(
        code: String,
        site: String = "test",
    ) = BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse(code)))

    private fun entry(
        code: String,
        title: String,
        site: String = "test",
    ) = BoardDirectoryEntry(board(code, site), title, "", 0, true)
}
