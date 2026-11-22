package com.solthariel.pawchan

import com.solthariel.pawchan.infrastructure.forumCookieOriginAllowed
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForumCookieScopeTest {
    private val origin = "https://2ch.su/".toHttpUrl()

    @Test fun cookieAccessRequiresTheSelectedSecureOrigin() {
        assertTrue(forumCookieOriginAllowed("https://2ch.su/b/res/123.html".toHttpUrl(), origin))
        listOf(
            "https://a.4cdn.org/",
            "https://i.4cdn.org/",
            "https://2ch.org/",
            "https://2ch.su.evil.test/",
            "http://2ch.su/",
            "https://2ch.su:444/",
        ).forEach {
            assertFalse(it, forumCookieOriginAllowed(it.toHttpUrl(), origin))
        }
    }
}
