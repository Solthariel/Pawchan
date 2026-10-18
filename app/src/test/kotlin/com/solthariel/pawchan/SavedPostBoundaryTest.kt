package com.solthariel.pawchan

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
import com.solthariel.pawchan.domain.workspace.StoredPost
import com.solthariel.pawchan.domain.workspace.StoredReply
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.thread.SavedPostPresentation
import com.solthariel.pawchan.workspace.prepareWorkspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedPostBoundaryTest {
    @Test fun preparedEntriesKeepFullSiteBoardThreadPostIdentity() {
        val first = address("dvach")
        val second = address("other")
        val state =
            WorkspaceState(
                posts =
                    listOf(
                        StoredPost(post(first, 5), "A", own = true),
                        StoredPost(post(second, 5), "B", own = true),
                    ),
            )
        val prepared = prepareWorkspace(state)
        assertEquals(2, prepared.own.size)
        assertEquals(setOf(first, second), prepared.own.map { it.presentation.address.thread }.toSet())
        assertEquals(listOf(5L, 5L), prepared.own.map { it.id })
    }

    @Test fun replyLabelIsPreparedWithLocalGroupAndUnreadLabelIsPreserved() {
        val address = address("dvach")
        val source = post(address, 5)
        val reference = PostTextRun(">>5", link = PostLink.Reference(requireNotNull(PostId.parse(5))))
        val reply = post(address, 6, PostDocument(listOf(reference)))
        val result =
            prepareWorkspace(
                WorkspaceState(
                    posts = listOf(StoredPost(source, "Title", own = true)),
                    replies = listOf(StoredReply(reply, "Title", true)),
                ),
            ).own.single()
        assertEquals("1 ответ", result.presentation.replyLabel)
        assertEquals("/b/ Title • новых: 1", result.label)
    }

    @Test fun ownMembershipIsNotChangedByRendererBoundary() {
        val address = address("dvach")
        val result =
            prepareWorkspace(
                WorkspaceState(
                    posts =
                        listOf(
                            StoredPost(post(address, 5), "A", own = true),
                            StoredPost(post(address, 6), "B", own = false),
                            StoredPost(post(address, 7), "C", own = true),
                        ),
                ),
            )
        assertEquals(listOf(5L, 7L), result.own.map { it.id })
    }

    @Test fun emptyThreadHasNoPreparedCardsAndNoSyntheticAddress() {
        val snapshot = ThreadSnapshot(address("dvach"), null, ThreadFlags(false, false, false), emptyList())
        val result = SavedPostPresentation.prepare(snapshot)
        assertTrue(result.isEmpty())
    }

    private fun address(site: String): ThreadAddress =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )

    private fun post(
        address: ThreadAddress,
        id: Long,
        document: PostDocument = PostDocument.plain("Text"),
    ): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(id))),
            PostContent(document, PostMetadata(null, null, false)),
            emptyList(),
        )
}
