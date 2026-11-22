package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.infrastructure.sites.DvachWebLinks
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import com.solthariel.pawchan.infrastructure.sites.FourChanWebLinks
import com.solthariel.pawchan.infrastructure.thread.AppPostLinkResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI

class FourChanWebLinksTest {
    private val adapter = FourChanWebLinks()
    private val board = BoardAddress(requireNotNull(SiteId.parse("4chan")), requireNotNull(BoardId.parse("g")))
    private val thread = ThreadAddress(board, requireNotNull(ThreadId.parse(123)))

    @Test fun threadLinksRoundTripAcrossBothOfficialHosts() {
        assertEquals("https://boards.4chan.org/g/thread/123#p456", adapter.threadUrl(thread, 456))
        for (host in listOf("boards.4chan.org", "boards.4channel.org")) {
            assertEquals(ForumWebLink.Thread(thread, 456), adapter.webLink(URI("https://$host/g/thread/123#p456")))
            assertEquals(ForumWebLink.Board(board), adapter.webLink(URI("https://$host/g/catalog")))
        }
        assertEquals(
            ForumWebLink.Thread(thread, null),
            adapter.webLink(URI("https://boards.4chan.org/g/thread/123#p123")),
        )
    }

    @Test fun inputRetainsSiteAndInterpretsBareNumbersInContext() {
        assertEquals(ForumWebLink.Thread(thread, null), adapter.addressInput("123", board))
        assertEquals(ForumWebLink.Thread(thread, 456), adapter.addressInput("/g/thread/123#p456", board))
        assertEquals(ForumWebLink.Board(board), adapter.addressInput("g", board))
        assertNull(adapter.addressInput("123", null))
        val resolver = AppPostLinkResolver(listOf(DvachWebLinks(), adapter))
        assertEquals(ForumWebLink.Board(board), resolver.addressInput("g", board))
        assertEquals(
            ForumWebLink.Thread(thread, 456),
            resolver.addressInput("boards.4chan.org/g/thread/123#p456", null),
        )
    }

    @Test fun untrustedOriginsAndInvalidPathsAreNotForumTargets() {
        for (url in listOf(
            "https://boards.4chan.org.evil.test/g/thread/123",
            "https://user@boards.4chan.org/g/thread/123",
            "https://boards.4chan.org:444/g/thread/123",
            "http://boards.4chan.org/g/thread/123",
            "https://boards.4chan.org/g%2fthread/123",
            "https://boards.4chan.org/g/thread/0",
            "https://i.4cdn.org/g/not-a-file.jpg",
        )) {
            assertNull(adapter.webLink(URI(url)))
        }
        assertNull(adapter.threadUrl(thread, 0))
    }

    @Test fun mediaLinksNeverInventAThread() {
        assertEquals(ForumWebLink.Media(MediaKind.VIDEO), adapter.webLink(URI("https://i.4cdn.org/g/123.webm")))
        assertEquals(ForumWebLink.Media(MediaKind.IMAGE), adapter.webLink(URI("https://i.4cdn.org/g/123.jpg")))
        assertNull(adapter.webLink(URI("https://i.4cdn.org/g/123.exe")))
    }
}
