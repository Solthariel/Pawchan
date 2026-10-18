package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.domain.workspace.observe
import com.solthariel.pawchan.domain.workspace.storePost
import com.solthariel.pawchan.domain.workspace.withRepliesDismissed
import com.solthariel.pawchan.navigation.threadAddress
import com.solthariel.pawchan.threads.ThreadCounter
import com.solthariel.pawchan.threads.firstUnreadReply
import com.solthariel.pawchan.threads.newPostsCount
import com.solthariel.pawchan.threads.replyRows
import com.solthariel.pawchan.threads.unreadReplies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadsHubModelTest {
    private val address = requireNotNull(threadAddress("dvach", "b", 100L))

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
        ThreadSnapshot(address, "Тред", ThreadFlags(false, false, false), posts.toList())

    @Test
    fun repliesSectionCountsUnreadRepliesAndHidesDismissed() {
        val own = post(101)
        val reference = PostLink.Reference(own.address.post)
        val replies = listOf(post(102, reference), post(103, reference))
        val state =
            WorkspaceState()
                .storePost(own, "Тред", own = true)
                .observe(snapshot(own, *replies.toTypedArray()))
        assertEquals(2, replyRows(state).single().counter.count)
        assertEquals(2, unreadReplies(state))
        assertEquals(102L, firstUnreadReply(state, address))
        assertTrue(replyRows(state.withRepliesDismissed(address, true)).isEmpty())
    }

    @Test
    fun newPostsAreCountedAfterLastVisitWithoutDeleted() {
        val thread = snapshot(post(101), post(102), post(103))
        assertEquals(2, newPostsCount(thread, 101L))
        assertEquals(0, newPostsCount(thread, null))
        val deleted = thread.withPosts(thread.posts, setOf(requireNotNull(PostId.parse(103))))
        assertEquals(1, newPostsCount(deleted, 101L))
    }

    @Test
    fun counterLabelsFollowDashchan() {
        assertEquals("5", ThreadCounter(5).label)
        assertEquals("5X", ThreadCounter(5, deleted = true).label)
        assertEquals("X", ThreadCounter(deleted = true).label)
        assertEquals("?", ThreadCounter(error = true).label)
        assertEquals("1K+", ThreadCounter(1500).label)
        assertEquals(false, ThreadCounter().visible)
    }

    @Test
    fun rowCellShowsZeroLikeDashchanWatcher() {
        assertEquals("0", ThreadCounter().cellLabel)
        assertEquals("", ThreadCounter(running = true).cellLabel)
        assertEquals("7", ThreadCounter(7).cellLabel)
        assertEquals("X", ThreadCounter(deleted = true).cellLabel)
    }
}
