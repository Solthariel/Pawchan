package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.mergeThreadHistory
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.toThreadUiState
import com.solthariel.pawchan.thread.withPostHistory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPostHistoryTest {
    private val address = defaultThreadAddress(1)

    @Test fun firstLoadIsNotMarkedNew() {
        val current =
            toolsContent(
                listOf(toolsPost(1), toolsPost(2)),
            ).withPostHistory(null, emptySet()) as ThreadUiState.Content
        assertFalse(current.posts.any { it.metadata.changes.new })
    }

    @Test fun deletedPostsStayInPlaceWithXIndexAndLivePostsAreNumberedWithoutThem() {
        val stored = snapshot(1L to "ОП", 2L to "Два", 3L to "Три")
        val merged = mergeThreadHistory(stored, snapshot(1L to "Изменён", 3L to "Три", 4L to "Четыре"))
        val previous = DomainOutcome.Success(stored).toThreadUiState()
        val current =
            DomainOutcome.Success(merged).toThreadUiState().withPostHistory(previous, setOf(4)) as ThreadUiState.Content
        val byId = current.posts.associateBy { it.id }
        assertEquals(listOf(1L, 2L, 3L, 4L), current.posts.map { it.id })
        assertTrue(
            byId
                .getValue(1)
                .metadata.changes.edited,
        )
        assertTrue(
            byId
                .getValue(2)
                .metadata.changes.deleted,
        )
        assertEquals(
            0,
            byId
                .getValue(2)
                .metadata.display.index,
        )
        assertEquals(
            2,
            byId
                .getValue(3)
                .metadata.display.index,
        )
        assertEquals(
            3,
            byId
                .getValue(4)
                .metadata.display.index,
        )
        assertTrue(
            byId
                .getValue(4)
                .metadata.changes.new,
        )
        assertTrue(
            byId
                .getValue(4)
                .metadata.changes.own,
        )
    }

    private fun snapshot(vararg posts: Pair<Long, String>): ThreadSnapshot =
        ThreadSnapshot(
            address,
            "Тред",
            ThreadFlags(pinned = false, closed = false, archived = false),
            posts.map { (id, text) ->
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(id))),
                    PostContent(PostDocument.plain(text), PostMetadata("Аноним", null, false)),
                    emptyList(),
                )
            },
        )
}
