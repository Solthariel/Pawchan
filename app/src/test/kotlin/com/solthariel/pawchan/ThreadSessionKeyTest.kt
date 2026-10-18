package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.navigation.threadSessionKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ThreadSessionKeyTest {
    @Test
    fun threadAndReplyHaveDifferentRolesInTheSameOwner() {
        val address = address("dvach", "b")
        assertNotEquals(threadSessionKey("thread", address), threadSessionKey("reply", address))
    }

    @Test
    fun equalNumbersAcrossSitesOrBoardsDoNotShareASession() {
        val first = threadSessionKey("reply", address("source-one", "b"))
        assertNotEquals(first, threadSessionKey("reply", address("source-two", "b")))
        assertNotEquals(first, threadSessionKey("reply", address("source-one", "c")))
    }

    @Test
    fun hyphensInsideIdentifiersCannotCreateACollision() {
        assertNotEquals(
            threadSessionKey("thread", address("a-b", "c")),
            threadSessionKey("thread", address("a", "b-c")),
        )
        assertEquals("thread:a-b/c/42", threadSessionKey("thread", address("a-b", "c")))
    }

    private fun address(
        site: String,
        board: String,
    ): ThreadAddress =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse(board))),
            requireNotNull(ThreadId.parse(42)),
        )
}
