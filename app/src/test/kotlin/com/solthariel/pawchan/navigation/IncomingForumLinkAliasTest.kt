package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.infrastructure.sites.DvachWebLinks
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI

class IncomingForumLinkAliasTest {
    private val aliases = listOf("2ch.hk", "2ch.su", "2ch.life", "2ch.org")

    @Test fun everyAliasAgreesWithTheInternalParserForBoardsAndThreads() {
        aliases.forEach { host ->
            listOf("/b/", "/b/catalog.html", "/b/res/123.html#p456").forEach { path ->
                val url = "https://$host$path"
                val expected = requireNotNull(DvachWebLinks().webLink(URI(url)))
                assertEquals(url, expected, incomingForumLink(url))
            }
        }
    }

    @Test fun historicalAliasRetainsTheBoardAndEveryPostAnchorFormat() {
        assertEquals(ForumWebLink.Board(defaultBoardAddress()), incomingForumLink("http://2ch.hk/b/"))
        listOf("456", "p456", "post456").forEach { anchor ->
            assertEquals(
                ForumWebLink.Thread(defaultThreadAddress(123L), 456L),
                incomingForumLink("https://2ch.hk/b/res/123.html#$anchor"),
            )
        }
    }

    @Test fun caseAndDefaultPortsRetainTheHistoricalThreadIdentity() {
        listOf("HTTPS://2CH.HK:443", "http://2ch.hk:80").forEach { origin ->
            assertEquals(
                ForumWebLink.Thread(defaultThreadAddress(123L), 456L),
                incomingForumLink("$origin/b/res/123.html?from=share#456"),
            )
        }
    }

    @Test fun aliasesCannotBeUsedToSmuggleAnotherOriginOrCredentials() {
        aliases.forEach { host ->
            listOf(
                "https://$host.evil.example/b/res/123.html",
                "https://evil.example/$host/b/res/123.html",
                "https://$host@evil.example/b/res/123.html",
                "https://user:password@$host/b/res/123.html",
                "https://$host./b/res/123.html",
                "https://$host%2eevil.example/b/res/123.html",
            ).forEach { assertNull(it, incomingForumLink(it)) }
        }
    }

    @Test fun aliasesDoNotRelaxSchemeOrPortChecks() {
        aliases.forEach { host ->
            listOf(
                "https://$host:80/b/res/123.html",
                "http://$host:443/b/res/123.html",
                "https://$host:444/b/res/123.html",
                "ftp://$host/b/res/123.html",
                "//$host/b/res/123.html",
            ).forEach { assertNull(it, incomingForumLink(it)) }
        }
    }

    @Test fun historicalAliasDoesNotAdmitMediaOrServicePaths() {
        listOf(
            "/api/",
            "/static/",
            "/b/src/123/file.mp4",
            "/b/res/0.html",
            "/b/res/999999999999999999999.html",
            "/b/%2e%2e/res/123.html",
        ).forEach { path -> assertNull(path, incomingForumLink("https://2ch.hk$path")) }
    }

    @Test fun newMirrorsAreNotGrantedExternalEntryByAnAdapterSetting() {
        val custom = DvachWebLinks("mirror.example")
        val url = "https://mirror.example/b/res/123.html"
        assertEquals(ForumWebLink.Thread(defaultThreadAddress(123L), null), custom.webLink(URI(url)))
        assertNull(incomingForumLink(url))
        assertNull(incomingForumLink("https://2ch.pm/b/res/123.html"))
    }
}
