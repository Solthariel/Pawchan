package com.solthariel.pawchan

import com.solthariel.pawchan.infrastructure.parseCookieHeader
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidCookieJarTest {
    @Test
    fun parsesWebViewCookieHeaderForHttpsRequest() {
        val cookies =
            parseCookieHeader(
                header = "cf_clearance=token; usercode_auth=abc=123; malformed",
                url = "https://2ch.su/b/catalog.json".toHttpUrl(),
            )

        assertEquals(listOf("cf_clearance", "usercode_auth"), cookies.map { it.name })
        assertEquals("abc=123", cookies.last().value)
        assertTrue(cookies.all { it.secure })
    }
}
