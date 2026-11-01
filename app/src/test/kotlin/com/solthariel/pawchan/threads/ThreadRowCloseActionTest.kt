package com.solthariel.pawchan.threads

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadRowCloseActionTest {
    @Test fun eachCrossOnlyInvokesItsOwnLocalRemoval() {
        val address =
            ThreadAddress(
                BoardAddress(
                    requireNotNull(SiteId.parse("dvach")),
                    requireNotNull(BoardId.parse("b")),
                ),
                requireNotNull(ThreadId.parse(42L)),
            )
        val row = ThreadRowUi(address, "Thread")
        val calls = mutableListOf<String>()
        val actions =
            ThreadsScreenActions(
                { _, _ -> calls.add("open") },
                ThreadsPageActions({
                    assertEquals(address, it)
                    calls.add("close")
                }, { calls.add("closeAll") }),
                { target, favorite ->
                    assertEquals(row, target)
                    assertEquals(false, favorite)
                    calls.add("unfavorite")
                },
                {
                    assertEquals(address, it)
                    calls.add("dismiss")
                },
                { calls.add("copy") },
                {},
                ThreadsLinkActions({}, {}),
            )
        val expected = listOf("close", "unfavorite", "dismiss")
        ThreadsSection.entries.forEachIndexed { index, section ->
            calls.clear()
            threadRowCloseAction(section, row, actions).onClick()
            assertEquals(listOf(expected[index]), calls)
        }
        assertEquals(
            3,
            ThreadsSection.entries
                .map { threadRowCloseAction(it, row, actions).label }
                .toSet()
                .size,
        )
    }
}
