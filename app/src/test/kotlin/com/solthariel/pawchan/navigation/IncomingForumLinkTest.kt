package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingForumLinkTest {
    @Test fun allKnownMirrorsAcceptHttpAndHttpsThreadLinks() {
        listOf("2ch.hk", "2ch.su", "2ch.org", "2ch.life").forEach { host ->
            listOf("https", "http").forEach { scheme ->
                assertEquals(
                    ForumWebLink.Thread(defaultThreadAddress(123L), 456L),
                    incomingForumLink("$scheme://$host/b/res/123.html#456"),
                )
            }
        }
    }

    @Test fun boardsAndCatalogPagesOpenTheBoard() {
        listOf("/b", "/b/", "/b/index.html", "/b/catalog.html", "/b/2.html").forEach { path ->
            assertEquals(ForumWebLink.Board(defaultBoardAddress()), incomingForumLink("https://2ch.su$path"))
        }
    }

    @Test fun postAnchorFormatsArePreserved() {
        listOf("456", "p456", "post456").forEach { anchor ->
            val link = incomingForumLink("https://2ch.su/b/res/123.html#$anchor") as ForumWebLink.Thread
            assertEquals(456L, link.postId)
        }
        assertEquals(
            ForumWebLink.Thread(defaultThreadAddress(123L), null),
            incomingForumLink("https://2ch.su/b/res/123.html#123"),
        )
    }

    @Test fun existingNotificationLinksRemainSupported() {
        assertEquals(
            ForumWebLink.Thread(defaultThreadAddress(123L), null),
            incomingForumLink("pawchan://thread/dvach/b/123"),
        )
        assertNull(incomingForumLink("pawchan://thread/unknown/b/123"))
    }

    @Test fun replyNotificationHasAnExplicitIndependentEntryKind() {
        assertEquals(
            ForumWebLink.Thread(defaultThreadAddress(123L), null, replies = true),
            incomingForumLink("pawchan://thread/dvach/b/123?replies=1"),
        )
        assertEquals(
            ForumWebLink.Thread(defaultThreadAddress(123L), 456L),
            incomingForumLink("https://2ch.su/b/res/123.html?replies=1#456"),
        )
    }

    @Test fun otherOriginsAndCredentialsAreRejected() {
        listOf(
            "https://2ch.su.evil.example/b/res/123.html",
            "https://evil.example/2ch.su/b/res/123.html",
            "https://user@2ch.su/b/res/123.html",
            "https://2ch.su@evil.example/b/res/123.html",
            "https://2ch.su:444/b/res/123.html",
            "http://2ch.su:443/b/res/123.html",
            "file:///b/res/123.html",
            "intent://2ch.su/b/res/123.html",
            "https://invalid host/b/",
            "https://2ch.pm/b/res/123.html",
        ).forEach { assertNull(it, incomingForumLink(it)) }
    }

    @Test fun unsupportedPathsAndInvalidNumbersAreIgnored() {
        listOf(
            "https://2ch.su/",
            "https://2ch.su/api/",
            "https://2ch.su/b/res/0.html",
            "https://2ch.su/b/res/999999999999999999999.html",
            "https://2ch.su/b/src/123/file.mp4",
            "https://2ch.su/b/%2e%2e/res/123.html",
        ).forEach { assertNull(it, incomingForumLink(it)) }
    }

    @Test fun queryAndCaseDoNotLoseThePostAnchor() {
        assertEquals(
            ForumWebLink.Thread(defaultThreadAddress(123L), 456L),
            incomingForumLink("HTTPS://2CH.SU:443/b/res/123.html?from=share#456"),
        )
        assertEquals(ForumWebLink.Board(defaultBoardAddress()), incomingForumLink("http://2ch.su:80/b/"))
    }
}
