package com.solthariel.pawchan.update

import kotlinx.coroutines.runBlocking
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AnonymousUpdateHttpClientTest {
    @Test fun ordinaryClientHasNoAuthenticationInterceptorsAndKeepsTlsRedirectPolicy() {
        val client = updateHttpClient()
        try {
            assertSame(Dns.SYSTEM, client.dns)
            assertTrue(client.interceptors.isEmpty())
            assertTrue(client.networkInterceptors.isEmpty())
            assertFalse(client.followSslRedirects)
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    @Test fun clientCanBorrowProxyRoutingWithoutBorrowingCookies() {
        val selector = java.net.ProxySelector.getDefault()
        val guard = okhttp3.Interceptor { chain -> chain.proceed(chain.request()) }
        val client =
            updateHttpClient {
                proxySelector(selector)
                addNetworkInterceptor(guard)
            }
        try {
            org.junit.Assert.assertSame(selector, client.proxySelector)
            org.junit.Assert.assertSame(okhttp3.CookieJar.NO_COOKIES, client.cookieJar)
            assertEquals(listOf(guard), client.networkInterceptors)
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }

    @Test fun publicFeedIsRequestedUnchangedWithoutAuthorization() =
        runBlocking {
            val url = "https://raw.githubusercontent.com/owner/repo/master/update/feed/data-v1.json".toHttpUrl()
            val client =
                updateHttpClient()
                    .newBuilder()
                    .addInterceptor { chain ->
                        assertEquals(url, chain.request().url)
                        assertNull(chain.request().header("Authorization"))
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(200)
                            .message("OK")
                            .body("feed".toResponseBody())
                            .build()
                    }.build()
            UpdateTransport(client).use { transport -> assertEquals("feed", transport.readText(url)) }
        }

    @Test fun inaccessibleFeedIsAnOrdinaryFailure() =
        runBlocking {
            val client =
                updateHttpClient()
                    .newBuilder()
                    .addInterceptor { chain ->
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(404)
                            .message("Not Found")
                            .body("missing".toResponseBody())
                            .build()
                    }.build()
            UpdateTransport(client).use { transport ->
                val url = "https://example.org/private-feed".toHttpUrl()
                val error = runCatching { transport.readText(url) }.exceptionOrNull()
                assertTrue(error is IllegalStateException)
                assertEquals("Update request failed: 404", error?.message)
            }
        }
}
