package com.solthariel.pawchan

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.thread.PostPreviewState
import com.solthariel.pawchan.thread.PostPreviewTarget
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadPostUi
import com.solthariel.pawchan.thread.ThreadPostVisibility
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.filteredContent
import com.solthariel.pawchan.thread.localPostPreview
import com.solthariel.pawchan.thread.retainPreview
import com.solthariel.pawchan.thread.withPreviewFilters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoHiddenPostRowsTest {
    private val address = defaultThreadAddress(1)
    private val rules =
        listOf(
            ContentFilterRule(
                "auto",
                address.site.value,
                condition = ContentFilterConditionSpec(values = listOf("скрыть")),
            ),
        )

    @Test
    fun omittedRowsDoNotReservePositionsOrLoseFilterHitsAndOrdinaryReplies() {
        val raw = content(reply(2, "скрыть"), reply(3, "оставить"))
        val before = raw.rows(true)
        val after = raw.rows(false)
        assertEquals(listOf(1L, 2L, 3L), before.posts.map { it.id })
        assertTrue(before.posts[1].metadata.hidden)
        assertEquals(listOf(1L, 3L), after.posts.map { it.id })
        assertEquals(mapOf(1L to 0, 3L to 1), after.presentation.postPositions)
        assertEquals(
            listOf(3L),
            after.presentation.replyIndex[1L]
                ?.posts
                ?.map { it.id },
        )
        assertEquals(before.presentation.contentFilters.hits, after.presentation.contentFilters.hits)
        assertEquals(1, after.presentation.filteredComments)
        assertEquals(before.statistics, after.statistics)
        assertEquals(before.presentation.filterSamples, after.presentation.filterSamples)
        assertEquals(before.presentation.contentFilterNotice, after.presentation.contentFilterNotice)
        assertFalse(raw.posts[1].metadata.hidden)
        val noRules = raw.filteredContent(emptyList(), showHiddenPosts = false) as ThreadUiState.Content
        assertEquals(raw.posts, noRules.posts)
    }

    @Test
    fun explicitReferenceGetsACollapsedPostWhileRepliesStayEmptyInLocalAndRemotePreviews() {
        val reply = reply(2, "скрыть")
        val raw = content(reply)
        val single = PostPreviewTarget(address, 2)
        val replies = PostPreviewTarget(address, 1, replies = true)
        val before = requireNotNull(localPostPreview(replies, raw.rows(true)))
        val filtered = raw.rows(false)
        val after = requireNotNull(localPostPreview(replies, filtered))
        assertEquals(listOf(2L), before.posts.map { it.id })
        assertTrue(after.posts.isEmpty())
        assertEquals(after, retainPreview(before, after))
        assertTrue(
            requireNotNull(localPostPreview(single, filtered))
                .posts
                .single()
                .metadata.hidden,
        )
        assertNull(localPostPreview(PostPreviewTarget(address, 999), filtered))
        val remote = PostPreviewState(content = raw, posts = listOf(reply), remote = true)
        assertTrue(
            remote
                .withPreviewFilters(rules, false)
                .posts
                .single()
                .metadata.hidden,
        )
        assertTrue(
            remote
                .copy(replies = true)
                .withPreviewFilters(rules, false)
                .posts
                .isEmpty(),
        )
        assertTrue(
            remote
                .copy(replies = true)
                .withPreviewFilters(rules, true)
                .posts
                .single()
                .metadata.hidden,
        )
    }

    @Test
    fun manualHideAndOpeningPostAreKeptAndIntentionalRevealDoesNotEditRules() {
        val automatic = reply(2, "скрыть")
        val manual = automatic.copy(id = 3, metadata = automatic.metadata.copy(visibility = ThreadPostVisibility(true)))
        val raw = content(automatic, manual)
        val hidden = raw.rows(false)
        assertEquals(listOf(1L, 3L), hidden.posts.map { it.id })
        assertFalse(
            hidden.posts
                .first()
                .metadata.hidden,
        )
        assertTrue(
            hidden.posts
                .last()
                .metadata.hidden,
        )
        assertEquals(
            listOf(2L),
            hidden.presentation.contentFilters.hits
                .map { it.postId },
        )
        val revealed = raw.filteredContent(rules, setOf(2L, 3L), showHiddenPosts = false) as ThreadUiState.Content
        assertEquals(listOf(false, false, true), revealed.posts.map { it.metadata.hidden })
        assertTrue(rules.single().enabled)
    }

    private fun ThreadUiState.Content.rows(show: Boolean): ThreadUiState.Content =
        filteredContent(rules, showHiddenPosts = show) as ThreadUiState.Content

    private fun content(vararg replies: ThreadPostUi): ThreadUiState.Content =
        toolsContent(listOf(toolsPost(1, "скрыть")) + replies.toList())
            .copy(address = address, statistics = ThreadContentStatistics(replies = replies.size))

    private fun reply(
        id: Long,
        text: String,
    ): ThreadPostUi {
        val reference = PostLink.Reference(requireNotNull(PostId.parse(1)))
        val document = PostDocument(listOf(PostTextRun(">>1", link = reference), PostTextRun(" $text")))
        return toolsPost(id, text).copy(document = document, richText = RichPostTextUi.create(document))
    }
}
