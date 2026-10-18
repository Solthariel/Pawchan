package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.PostStateMarker
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDetails
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.thread.ThreadOpeningInfo
import com.solthariel.pawchan.thread.preparePostDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostHeaderDisplayTest {
    private val opening =
        ThreadOpeningInfo(ThreadFlags(pinned = true, closed = true, archived = false, endless = true), "tag")

    @Test fun threadStateAndTagBelongOnlyToOpeningPost() {
        val first = preparePostDisplay(post(42, PostDetails(email = "a@b", banned = true)), 1, opening)
        assertEquals(
            listOf(
                PostStateMarker.ORIGINAL_POSTER,
                PostStateMarker.EMAIL,
                PostStateMarker.STICKY,
                PostStateMarker.CLOSED,
                PostStateMarker.CYCLICAL,
                PostStateMarker.BANNED,
            ),
            first.markers,
        )
        assertEquals("tag", first.tag)
        val reply = preparePostDisplay(post(43), 2, opening)
        assertEquals(emptyList<PostStateMarker>(), reply.markers)
        assertNull(reply.tag)
    }

    @Test fun bylineKeepsTimestampTripAndDefaultNameSeparately() {
        val display = preparePostDisplay(post(43, PostDetails(timestamp = 100L, tripcode = "!trip"), "Аноним"), 2)
        assertNull(display.byline.author)
        assertEquals("Аноним", display.byline.defaultName)
        assertEquals(100L, display.byline.timestamp)
        assertEquals("!trip", display.byline.tripcode)
    }

    @Test fun subjectRepeatingCommentStartIsHidden() {
        val display = preparePostDisplay(post(43, subject = "text"), 2)
        assertNull(display.subject)
    }

    private fun post(
        id: Long,
        details: PostDetails = PostDetails(),
        name: String? = null,
        subject: String? = null,
    ): BoardPost =
        BoardPost(
            PostAddress(requireNotNull(threadAddress("dvach", "b", 42)), requireNotNull(PostId.parse(id))),
            PostContent(
                PostDocument.plain("text"),
                PostMetadata(name, "10.10.2026 11:12:13", false, subject = subject),
                details = details,
            ),
            emptyList(),
        )
}
