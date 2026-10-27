package com.solthariel.pawchan

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadPostVisibility
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.filteredContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ThreadContentFilterTest {
    private val address =
        ThreadAddress(
            BoardAddress(checkNotNull(SiteId.parse("2ch")), checkNotNull(BoardId.parse("b"))),
            checkNotNull(ThreadId.parse(1)),
        )
    private val rule =
        ContentFilterRule(
            "r",
            "2ch",
            condition = ContentFilterConditionSpec(values = listOf("скрыть")),
        )

    @Before
    fun ruleUsesSupportedFormat() {
        assertNull(contentFilterValidationError(rule))
    }

    @Test
    fun matchingReplyKeepsNonmatchingOpAndNavigation() {
        val raw =
            toolsContent(listOf(toolsPost(1, "оставить"), toolsPost(2, "скрыть"), toolsPost(3, "оставить")))
                .copy(address = address)
        val filtered = raw.filteredContent(listOf(rule)) as ThreadUiState.Content
        assertFalse(filtered.posts[0].metadata.hidden)
        assertTrue(filtered.posts[1].metadata.hidden)
        assertFalse(filtered.posts[2].metadata.hidden)
        assertNull(filtered.presentation.contentFilterNotice)
        assertEquals(1, filtered.presentation.filteredComments)
        assertEquals(1, filtered.presentation.postPositions[2L])
        assertEquals(3, filtered.presentation.filterSamples.size)
        assertFalse(raw.posts[1].metadata.hidden)
        val hit =
            filtered.presentation.contentFilters.hits
                .single()
        assertEquals("r", hit.ruleId)
        assertEquals(address, hit.thread)
        assertEquals(2L, hit.postId)
        assertFalse(hit.originalPost)
    }

    @Test
    fun revealIsTemporaryAndDoesNotUndoManualHiding() {
        val post = toolsPost(3, "скрыть")
        val hidden = post.copy(metadata = post.metadata.copy(visibility = ThreadPostVisibility(hidden = true)))
        val raw = toolsContent(listOf(toolsPost(1), toolsPost(2, "скрыть"), hidden)).copy(address = address)
        val revealed = raw.filteredContent(listOf(rule), setOf(2L, 3L)) as ThreadUiState.Content
        assertFalse(revealed.posts[1].metadata.hidden)
        assertTrue(revealed.posts[2].metadata.hidden)
        assertNull(revealed.posts[1].metadata.contentFilterLabel)
        assertTrue(
            revealed.presentation.contentFilters.hits
                .isEmpty(),
        )
        assertTrue((raw.filteredContent(listOf(rule)) as ThreadUiState.Content).posts[1].metadata.hidden)
        assertFalse((raw.filteredContent(emptyList()) as ThreadUiState.Content).posts[1].metadata.hidden)
    }

    @Test
    fun hiddenMediaReturnsOnRevealWithoutChangingCountsOrPostPositions() {
        val attachment =
            MediaAttachmentUi(
                MediaAttachmentSource("image", "image.jpg", "https://example.com/image.jpg", null, 2L),
                12L,
                AttachmentViewer.IMAGE,
                MediaKind.IMAGE,
            )
        val original = toolsPost(2, "скрыть")
        val reply = original.copy(attachments = listOf(attachment), attachmentRows = listOf(listOf(attachment)))
        val raw =
            toolsContent(listOf(toolsPost(1), reply)).copy(
                address = address,
                statistics = ThreadContentStatistics(replies = 1, files = 1, postsWithFiles = 1, mediaBytes = 12L),
            )
        val filtered = raw.filteredContent(listOf(rule)) as ThreadUiState.Content
        val hiddenGallery = filtered.presentation.gallery.all
        assertTrue(hiddenGallery.items.isEmpty())
        assertFalse(filtered.presentation.mediaGroups.containsKey(2L))
        assertEquals(1, filtered.presentation.postPositions[2L])
        assertEquals(raw.statistics, filtered.statistics)
        assertEquals("r", filtered.posts[1].metadata.contentFilterId)
        val revealed = raw.filteredContent(listOf(rule), setOf(2L)) as ThreadUiState.Content
        val revealedGallery = revealed.presentation.gallery.all
        assertEquals(listOf(attachment), revealedGallery.items)
        assertEquals(listOf(attachment), revealed.presentation.mediaGroups[2L])
        assertEquals(filtered.presentation.postPositions, revealed.presentation.postPositions)
        assertEquals(raw.statistics, revealed.statistics)
        assertNull(revealed.posts[1].metadata.contentFilterId)
        assertFalse(raw.posts[1].metadata.hidden)
    }

    @Test
    fun matchingOpProducesNoticeAndTheSameRuleHidesReply() {
        val raw = toolsContent(listOf(toolsPost(1, "скрыть"), toolsPost(2, "скрыть"))).copy(address = address)
        val filtered = raw.filteredContent(listOf(rule)) as ThreadUiState.Content
        assertEquals(rule.label, filtered.presentation.contentFilterNotice)
        assertFalse(
            filtered.posts
                .first()
                .metadata.hidden,
        )
        assertTrue(
            filtered.posts
                .last()
                .metadata.hidden,
        )
        assertEquals(listOf(1L, 2L), filtered.posts.map { it.id })
        assertEquals(1, filtered.presentation.filteredComments)
        assertEquals(
            listOf(2L),
            filtered.presentation.contentFilters.hits
                .map { it.postId },
        )
    }

    @Test
    fun obsoleteReplyOnlyRuleIsRejectedAndDoesNotHidePosts() {
        val obsolete = rule.copy(targets = ContentFilterTargets(threads = false, comments = true))
        assertNotNull(contentFilterValidationError(obsolete))
        val raw = toolsContent(listOf(toolsPost(1, "скрыть"), toolsPost(2, "скрыть"))).copy(address = address)
        val filtered = raw.filteredContent(listOf(obsolete)) as ThreadUiState.Content
        assertTrue(filtered.posts.none { it.metadata.hidden })
        assertNull(filtered.presentation.contentFilterNotice)
        assertTrue(
            filtered.presentation.contentFilters.hits
                .isEmpty(),
        )
        assertEquals(0, filtered.presentation.filteredComments)
    }

    @Test
    fun manualHidingAndEditorProbesDoNotInventStatisticsEvents() {
        val original = toolsPost(2, "скрыть")
        val hidden = original.copy(metadata = original.metadata.copy(visibility = ThreadPostVisibility(hidden = true)))
        val raw = toolsContent(listOf(toolsPost(1), hidden)).copy(address = address)
        val filtered = raw.filteredContent(listOf(rule)) as ThreadUiState.Content
        assertTrue(filtered.posts[1].metadata.hidden)
        assertTrue(
            filtered.presentation.contentFilters.hits
                .isEmpty(),
        )
    }
}
