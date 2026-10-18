package com.solthariel.pawchan

import com.solthariel.pawchan.boards.BoardLock
import com.solthariel.pawchan.boards.boardStatus
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardsStatusTest {
    @Test
    fun readOnlyBoardShowsOnlyLock() {
        val status = boardStatus(board(postingEnabled = false), postsPerHour = 0, unavailable = false)

        assertNull(status.postsPerHour)
        assertEquals(BoardLock.READ_ONLY, status.lock)
    }

    @Test
    fun availableBoardShowsEvenZeroSpeed() {
        val status = boardStatus(board(postingEnabled = true), postsPerHour = 0, unavailable = false)

        assertEquals(0, status.postsPerHour)
        assertNull(status.lock)
    }

    @Test
    fun unavailableBoardShowsRestrictedLockInsteadOfSpeed() {
        val status = boardStatus(board(postingEnabled = true), postsPerHour = 10, unavailable = true)

        assertNull(status.postsPerHour)
        assertEquals(BoardLock.RESTRICTED, status.lock)
    }

    private fun board(postingEnabled: Boolean): BoardDirectoryEntry =
        BoardDirectoryEntry(
            address =
                BoardAddress(
                    site = requireNotNull(SiteId.parse("dvach")),
                    board = requireNotNull(BoardId.parse("test")),
                ),
            title = "Тест",
            category = "Разное",
            postsPerHour = 0,
            postingEnabled = postingEnabled,
        )
}
