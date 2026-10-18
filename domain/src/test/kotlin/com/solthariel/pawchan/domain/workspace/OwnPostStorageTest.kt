package com.solthariel.pawchan.domain.workspace

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OwnPostStorageTest {
    @Test fun repeatedStorageDoesNotDuplicateOwnPost() {
        val source = post()
        val state = WorkspaceState().storePost(source, "first", true).storePost(source, "next", false)
        assertEquals(1, state.posts.size)
        assertTrue(state.posts.single().own)
        assertEquals("next", state.posts.single().title)
    }

    @Test fun updatingPostPreservesTrackingChoice() {
        val source = post()
        val state = WorkspaceState(posts = listOf(StoredPost(source, "old", own = true, tracking = false)))
        assertFalse(
            state
                .storePost(source, "new", true)
                .posts
                .single()
                .tracking,
        )
    }

    @Test fun storingPostDoesNotChangeOpenTabs() {
        val source = post()
        val tabs = listOf(OpenThread(source.address.thread, "tab"))
        val state = WorkspaceState(tabs = tabs, lastRoute = "route").storePost(source, "title", true)
        assertEquals(tabs, state.tabs)
        assertEquals("route", state.lastRoute)
    }

    @Test fun equalNumbersOnDifferentBoardsAndSitesAreIndependent() {
        val state =
            WorkspaceState()
                .storePost(post(), null, true)
                .storePost(post(site = "other"), null, true)
                .storePost(post(board = "other"), null, true)
        assertEquals(3, state.posts.size)
        assertEquals(
            3,
            state.posts
                .map { it.post.address }
                .distinct()
                .size,
        )
    }

    @Test fun ownMembershipCanBeAddedToAnExistingTrackedPost() {
        val source = post()
        val state = WorkspaceState().storePost(source, "first", false).storePost(source, "new", true)
        assertEquals(1, state.posts.size)
        assertTrue(state.posts.single().own)
    }

    private fun post(
        site: String = "dvach",
        board: String = "b",
    ): BoardPost =
        BoardPost(
            PostAddress(
                ThreadAddress(
                    BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse(board))),
                    requireNotNull(ThreadId.parse(42)),
                ),
                requireNotNull(PostId.parse(44)),
            ),
            PostContent(PostDocument.plain("text"), PostMetadata(null, null, false)),
            emptyList(),
        )
}
