package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.thread.preparePostDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostingMarkerDisplayTest {
    @Test fun markersLeaveCopyAndExportHeaderIntact() {
        val display = preparePostDisplay(post(42, true), 1)
        assertTrue(display.header.text.contains(" OP sage"))
        assertEquals(null, display.byline.author)
        assertEquals("10.10 11:12:13", display.byline.date)
        assertTrue(display.originalPoster)
        assertTrue(display.sage)
    }

    @Test fun authorNameContainingMarkerWordsIsNotRemoved() {
        val display = preparePostDisplay(post(43, false, "OP sage"), 2)
        assertEquals("OP sage", display.byline.author)
        assertEquals(null, display.byline.authorColor)
        assertTrue(display.header.text.contains("#43 OP sage 10.10"))
        assertFalse(display.originalPoster)
        assertFalse(display.sage)
    }

    private fun post(
        id: Long,
        sage: Boolean,
        name: String? = null,
    ): BoardPost =
        BoardPost(
            PostAddress(requireNotNull(threadAddress("dvach", "b", 42)), requireNotNull(PostId.parse(id))),
            PostContent(PostDocument.plain("text"), PostMetadata(name, "10.10.2026 11:12:13", sage)),
            emptyList(),
        )
}
