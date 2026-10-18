package com.solthariel.pawchan.domain.workspace

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(100)),
        )

    private fun post(
        id: Long,
        reference: PostLink.Reference? = null,
    ): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(id))),
            PostContent(PostDocument(listOf(PostTextRun("text", link = reference))), PostMetadata(null, null, false)),
            emptyList(),
        )

    private fun snapshot(vararg posts: BoardPost): ThreadSnapshot =
        ThreadSnapshot(address, "title", ThreadFlags(false, false, false), posts.toList())

    @Test fun discoversReplyAndDoesNotMakeReadReplyUnreadAgain() {
        val own = post(101)
        val reply = post(102, PostLink.Reference(own.address.post))
        val stored = WorkspaceState().storePost(own, "title", own = true).observe(snapshot(own, reply))
        assertTrue(stored.replies.single().unread)
        val read = stored.markRead(setOf(reply.address)).observe(snapshot(own, reply))
        assertFalse(read.replies.single().unread)
        assertEquals(1, read.replies.size)
    }

    @Test fun explicitDifferentThreadReferenceDoesNotCountAsReply() {
        val own = post(101)
        val other = address.copy(thread = requireNotNull(ThreadId.parse(200)))
        val reply = post(102, PostLink.Reference(own.address.post, other))
        val stored = WorkspaceState().storePost(own, "title", own = true).observe(snapshot(own, reply))
        assertTrue(stored.replies.isEmpty())
    }

    @Test fun storingTheSameOwnPostPreservesIdentityWithoutDuplicates() {
        val own = post(101)
        val state = WorkspaceState().storePost(own, "title", own = true).storePost(own, "title")
        assertEquals(1, state.posts.size)
        assertTrue(state.posts.single().own)
    }

    @Test fun serverRefreshPreservesSavedPostIfOriginalDisappears() {
        val own = post(101)
        val state = WorkspaceState().storePost(own, "title", own = true).observe(snapshot(post(100)))
        assertEquals(
            own.address,
            state.posts
                .single()
                .post.address,
        )
        assertEquals(
            "text",
            state.posts
                .single()
                .post.content.document.plainText,
        )
    }

    @Test fun visitReadsRepliesAndRemembersLastPost() {
        val own = post(101)
        val reply = post(102, PostLink.Reference(own.address.post))
        val visited =
            WorkspaceState()
                .storePost(own, "title", own = true)
                .observe(snapshot(own, reply))
                .visit(snapshot(own, reply))
        assertFalse(visited.replies.single().unread)
        assertEquals(102L, visited.marks.seen[address])
    }

    @Test fun ownPostReturnsDismissedThreadToReplies() {
        val dismissed = WorkspaceState().withRepliesDismissed(address, true)
        assertTrue(address in dismissed.marks.dismissedReplies)
        val posted = dismissed.storePost(post(103), "title", own = true)
        assertFalse(address in posted.marks.dismissedReplies)
    }

    @Test fun seenThreadsAreBounded() {
        var state = WorkspaceState()
        (1L..(SEEN_THREADS_LIMIT + 5L)).forEach { id ->
            val thread = address.copy(thread = requireNotNull(ThreadId.parse(id)))
            state = state.withSeen(thread, id)
        }
        assertEquals(SEEN_THREADS_LIMIT, state.marks.seen.size)
        assertEquals(
            SEEN_THREADS_LIMIT + 5L,
            state.marks.seen.values
                .last(),
        )
    }
}
