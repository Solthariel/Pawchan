package com.solthariel.pawchan

import com.solthariel.pawchan.infrastructure.GithubNetwork
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class GithubNetworkTest {
    @Test fun updateTransportBorrowsResourcesButNeverForumCookiesInterceptorsOrCache() {
        val forum =
            OkHttpClient
                .Builder()
                .cookieJar(object : CookieJar by CookieJar.NO_COOKIES {})
                .addInterceptor { chain ->
                    chain.proceed(
                        chain
                            .request()
                            .newBuilder()
                            .header("Forum", "secret")
                            .build(),
                    )
                }.build()
        val github = GithubNetwork(forum)
        val client = github.historyClient
        assertSame(forum.dispatcher, client.dispatcher)
        assertSame(forum.connectionPool, client.connectionPool)
        assertSame(CookieJar.NO_COOKIES, client.cookieJar)
        assertEquals(emptyList<Any>(), client.interceptors)
        assertEquals(emptyList<Any>(), client.networkInterceptors)
        assertEquals(null, client.cache)
        assertFalse(client.followSslRedirects)
        assertSame(github.dns, client.dns)
    }
}
