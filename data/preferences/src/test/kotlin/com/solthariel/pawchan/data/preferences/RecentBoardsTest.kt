package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentBoardsTest {
    @Test
    fun visitMovesBoardToFrontAndPreservesDifferentSites() {
        val result = updateRecentBoards("test/b\ntest/mobi\nother/b\ntest/b", board("mobi"))
        assertEquals(listOf(board("mobi"), board("b"), board("b", "other")), result)
    }

    @Test
    fun malformedRecordsAreIgnoredAndHistoryIsBounded() {
        val encoded = "broken\n//\na/b/c\n" + (0..150).joinToString("\n") { "test/b$it" }
        val result = updateRecentBoards(encoded, board("new"))
        assertEquals(100, result.size)
        assertEquals(board("new"), result.first())
        assertEquals(board("b98"), result.last())
    }

    private fun board(
        code: String,
        site: String = "test",
    ) = BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse(code)))
}
