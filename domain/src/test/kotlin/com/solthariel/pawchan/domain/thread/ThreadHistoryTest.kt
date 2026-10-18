package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadHistoryTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(1)),
        )

    @Test fun missingPostsBecomeDeletedAndReturningPostsAreRestored() {
        val stored = snapshot(1, 2, 3)
        val merged = mergeThreadHistory(stored, snapshot(1, 3, 4))
        assertEquals(listOf(1L, 2L, 3L, 4L), merged.ids())
        assertEquals(setOf(2L), merged.deletedPosts.map { it.value }.toSet())
        val restored = mergeThreadHistory(merged, snapshot(1, 2, 3, 4))
        assertTrue(restored.deletedPosts.isEmpty())
    }

    @Test fun firstLoadIsTakenAsIs() {
        val incoming = snapshot(1, 2)
        assertSame(incoming, mergeThreadHistory(null, incoming))
    }

    @Test fun clearingOldRemovesOnlyDeletedPostsRightAfterOpeningPost() {
        val history = mergeThreadHistory(snapshot(1, 2, 3, 4, 5, 6, endless = true), snapshot(1, 4, 6, endless = true))
        val old = history.withoutOldDeleted()
        assertEquals(listOf(1L, 4L, 5L, 6L), old.ids())
        assertEquals(setOf(5L), old.deletedPosts.map { it.value }.toSet())
        assertEquals(listOf(1L, 4L, 6L), history.withoutDeleted().ids())
        assertTrue(history.withoutDeleted().deletedPosts.isEmpty())
    }

    private fun ThreadSnapshot.ids() = posts.map { it.address.post.value }

    private fun snapshot(
        vararg ids: Long,
        endless: Boolean = false,
    ): ThreadSnapshot =
        ThreadSnapshot(
            address,
            "subject",
            ThreadFlags(pinned = false, closed = false, archived = false, endless = endless),
            ids.map {
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(it))),
                    PostContent(PostDocument(listOf(PostTextRun("text$it"))), PostMetadata(null, null, false)),
                    emptyList(),
                )
            },
        )
}
