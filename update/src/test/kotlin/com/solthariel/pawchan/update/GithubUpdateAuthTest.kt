package com.solthariel.pawchan.update

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class GithubUpdateAuthTest {
    private val secret = "github_pat_example_test_credential_only"
    private val auth = GithubUpdateAuth(GithubUpdateUrls("Solthariel/Pawchan")) { secret }

    @Test fun ownContentsApiReceivesRawAcceptAndBearerHeader() {
        val request =
            auth.authorize(
                request("https://api.github.com/repos/Solthariel/Pawchan/contents/update/feed/data-v1.json"),
            )
        assertEquals("Bearer $secret", request.header("Authorization"))
        assertEquals("application/vnd.github.raw+json", request.header("Accept"))
        assertEquals("no-store", request.header("Cache-Control"))
    }

    @Test fun assetDownloadAndTagLookupUseDifferentMediaTypes() {
        val base = "https://api.github.com/repos/Solthariel/Pawchan/releases/"
        assertEquals("application/octet-stream", auth.authorize(request(base + "assets/123")).header("Accept"))
        assertEquals("application/vnd.github+json", auth.authorize(request(base + "tags/v0.1.50")).header("Accept"))
    }

    @Test fun cdnRedirectNeverReceivesTheCredentialAndDoesNotReadTheStore() {
        val noRead =
            GithubUpdateAuth(GithubUpdateUrls("Solthariel/Pawchan")) {
                error("Must not read credentials for CDN")
            }
        val source =
            request(
                "https://release-assets.githubusercontent.com/signed/file",
            ).newBuilder().header("Authorization", "Bearer $secret").build()
        assertNull(noRead.authorize(source).header("Authorization"))
    }

    @Test fun otherRepositoriesAndUntrustedRedirectsAreRejected() {
        listOf("https://api.github.com/repos/Other/Pawchan/contents/file", "https://evil.invalid/file").forEach {
            assertThrows(IOException::class.java) { auth.authorize(request(it)) }
        }
    }

    @Test fun missingAccessFailsWithoutNetworkOrSecretInMessage() {
        val missing = GithubUpdateAuth(GithubUpdateUrls("Solthariel/Pawchan")) { null }
        assertThrows(UpdateAccessRequired::class.java) {
            missing.authorize(request("https://api.github.com/repos/Solthariel/Pawchan/contents/feed.json"))
        }
    }

    @Test fun redirectsToHttpAndWriteMethodsAreRejected() {
        assertThrows(IOException::class.java) {
            auth.authorize(request("http://release-assets.githubusercontent.com/file"))
        }
        assertThrows(IOException::class.java) {
            val write =
                request("https://api.github.com/repos/Solthariel/Pawchan/contents/file")
                    .newBuilder()
                    .delete()
                    .build()
            auth.authorize(write)
        }
    }

    private fun request(url: String): Request = Request.Builder().url(url).build()
}
