package com.solthariel.pawchan.threads

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadsSectionCounterTest {
    @Test fun pagesCountOpenThreadsRatherThanUnreadPosts() {
        val rows = listOf(row(1, 100), row(2, 0))
        assertEquals(2, threadsSectionCounter(ThreadsSection.PAGES, rows).count)
    }

    @Test fun favoritesAndRepliesKeepTheirUnreadTotals() {
        val rows = listOf(row(1, 5), row(2, 7))
        assertEquals(12, threadsSectionCounter(ThreadsSection.FAVORITES, rows).count)
        assertEquals(12, threadsSectionCounter(ThreadsSection.REPLIES, rows).count)
    }

    @Test fun emptyAndLoadingSectionsHaveZeroCounts() {
        ThreadsSection.entries.forEach {
            assertEquals(0, threadsSectionCounter(it, null).count)
            assertEquals(0, threadsSectionCounter(it, emptyList()).count)
        }
    }

    private fun row(
        id: Long,
        unread: Int,
    ): ThreadRowUi {
        val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b")))
        return ThreadRowUi(
            ThreadAddress(board, requireNotNull(ThreadId.parse(id))),
            "Thread $id",
            ThreadCounter(unread),
        )
    }
}
