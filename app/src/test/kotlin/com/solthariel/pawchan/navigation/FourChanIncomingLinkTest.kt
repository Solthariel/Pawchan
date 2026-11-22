package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FourChanIncomingLinkTest {
    private val board = BoardAddress(requireNotNull(SiteId.parse("4chan")), requireNotNull(BoardId.parse("g")))
    private val thread = ThreadAddress(board, requireNotNull(ThreadId.parse(123)))

    @Test fun browserAndNotificationEntriesRetainTheirSource() {
        assertEquals(ForumWebLink.Thread(thread, 456), incomingForumLink("https://boards.4chan.org/g/thread/123#p456"))
        assertEquals(ForumWebLink.Board(board), incomingForumLink("https://boards.4channel.org/g/"))
        assertEquals(ForumWebLink.Thread(thread, null), incomingForumLink("pawchan://thread/4chan/g/123"))
    }

    @Test fun encodedPathsForeignOriginsAndCredentialsStayRejected() {
        listOf(
            "https://boards.4chan.org/g%2fthread/123",
            "https://boards.4chan.org.evil.test/g/thread/123",
            "https://user@boards.4chan.org/g/thread/123",
            "https://boards.4chan.org:444/g/thread/123",
            "https:/g/thread/123",
        ).forEach { assertNull(it, incomingForumLink(it)) }
    }
}
