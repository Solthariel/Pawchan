package com.solthariel.pawchan.data.network

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MirrorRoutingInterceptorTest {
    @Test fun knownSourceRequestsUseSelectedMirrorWithoutChangingResource() {
        val routing = MirrorRoutingInterceptor("2ch.org")
        val request = Request.Builder().url("https://2ch.hk/b/res/42.json?x=1").build()
        assertEquals("https://2ch.org/b/res/42.json?x=1", routing.route(request).url.toString())
        routing.host = "board.example.org"
        assertEquals("board.example.org", routing.route(request).url.host)
    }

    @Test fun originAndRefererFollowMirrorButOtherHostsAreNotRedirected() {
        val routing = MirrorRoutingInterceptor("2ch.su")
        val request =
            Request
                .Builder()
                .url("https://2ch.org/user/posting")
                .header("Origin", "https://2ch.org")
                .header("Referer", "https://2ch.org/b/res/42.html")
                .build()
        val routed = routing.route(request)
        assertEquals("https://2ch.su", routed.header("Origin"))
        assertEquals("https://2ch.su/b/res/42.html", routed.header("Referer"))
        val outside = Request.Builder().url("https://media.example.org/a.jpg").build()
        assertSame(outside, routing.route(outside))
    }

    @Test fun changingMirrorDoesNotCopyAuthorizationOrCookieHeaders() {
        val routing = MirrorRoutingInterceptor("2ch.org")
        val original =
            Request
                .Builder()
                .url("https://2ch.su/b/catalog.json")
                .header("Cookie", "session=private")
                .header("Authorization", "Bearer private")
                .build()
        val routed = routing.route(original)
        assertEquals(null, routed.header("Cookie"))
        assertEquals(null, routed.header("Authorization"))
        assertEquals("https", routed.url.scheme)
    }
}
