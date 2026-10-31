package com.solthariel.pawchan.update

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class GithubUpdateUrlsTest {
    private val urls = GithubUpdateUrls("Solthariel/Pawchan")

    @Test fun privateRawContentUsesAuthenticatedContentsApi() {
        val url =
            urls.content(
                "https://raw.githubusercontent.com/Solthariel/Pawchan/master/update/feed/data-v1.json".toHttpUrl(),
            )
        assertEquals("api.github.com", url.host)
        assertEquals("/repos/Solthariel/Pawchan/contents/update/feed/data-v1.json", url.encodedPath)
        assertEquals("master", url.queryParameter("ref"))
    }

    @Test fun releaseDownloadUsesTagLookupAndAnAssetId() {
        val (lookup, name) =
            urls.releaseLookup(
                "https://github.com/Solthariel/Pawchan/releases/download/v0.1.50/Pawchan-0.1.50.apk".toHttpUrl(),
            )
        assertEquals("/repos/Solthariel/Pawchan/releases/tags/v0.1.50", lookup.encodedPath)
        assertEquals("Pawchan-0.1.50.apk", name)
        assertEquals("/repos/Solthariel/Pawchan/releases/assets/123", urls.asset(123).encodedPath)
    }

    @Test fun anotherRepositoryAndRawHostLookalikesAreRejected() {
        listOf(
            "https://raw.githubusercontent.com/Other/Pawchan/master/feed.json",
            "https://raw.githubusercontent.com.evil.invalid/Solthariel/Pawchan/master/feed.json",
            "https://example.org/feed.json",
        ).forEach { assertThrows(IllegalArgumentException::class.java) { urls.content(it.toHttpUrl()) } }
    }

    @Test fun cleartextCredentialsAndNonstandardPortsAreRejected() {
        listOf(
            "http://raw.githubusercontent.com/Solthariel/Pawchan/master/feed.json",
            "https://user:password@raw.githubusercontent.com/Solthariel/Pawchan/master/feed.json",
            "https://raw.githubusercontent.com:444/Solthariel/Pawchan/master/feed.json",
        ).forEach { assertThrows(IllegalArgumentException::class.java) { urls.content(it.toHttpUrl()) } }
    }

    @Test fun unrelatedApiPathsCannotReceiveTheCredential() {
        listOf("/user", "/repos/Other/Pawchan/contents/file", "/repos/Solthariel/Pawchan/issues").forEach {
            assertFalse(urls.isApi(("https://api.github.com" + it).toHttpUrl()))
        }
    }

    @Test fun missingOrNonpositiveAssetIdsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { urls.asset(0) }
        assertThrows(IllegalArgumentException::class.java) { urls.asset(-1) }
        assertFalse(urls.isApi("https://api.github.com/repos/Solthariel/Pawchan/releases/assets/not-an-id".toHttpUrl()))
    }
}
