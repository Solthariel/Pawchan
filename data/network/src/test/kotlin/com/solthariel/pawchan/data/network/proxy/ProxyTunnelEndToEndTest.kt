package com.solthariel.pawchan.data.network.proxy

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** SOCKS5 and HTTP CONNECT through local proxies; no public proxy and no trust bypass. */
class ProxyTunnelEndToEndTest {
    private val bed = ProxyTestBed()
    private val target get() = "arhivach.vc:${bed.resource.port}"

    @After fun tearDown() = bed.close()

    @Test fun socks5CarriesHttpsWithoutResolvingTheResourceLocally() {
        val proxy = bed.proxy(TestProxyKind.SOCKS5)
        assertEquals(RESOURCE_BODY, bed.client(ProxyRoute.Socks5(proxy.endpoint)).get(bed.resource.url()))
        assertEquals(listOf(target), proxy.targets)
        assertEquals(emptyList<String>(), bed.dns.lookups)
        assertEquals(1, bed.resource.requests.get())
    }

    @Test fun socks5SendsTheLoginAndPassword() {
        val proxy = bed.proxy(TestProxyKind.SOCKS5, LOGIN)
        assertEquals(RESOURCE_BODY, bed.client(ProxyRoute.Socks5(proxy.endpoint, LOGIN)).get(bed.resource.url()))
        assertEquals(listOf("алиса:pässw:rd"), proxy.logins)
        assertEquals(listOf(target), proxy.targets)
    }

    @Test fun socks5TellsAWrongLoginFromAMissingOne() {
        val proxy = bed.proxy(TestProxyKind.SOCKS5, LOGIN)
        val wrong = failureOf { bed.client(ProxyRoute.Socks5(proxy.endpoint, WRONG_LOGIN)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.AUTH_REJECTED, wrong.proxyFailure())
        assertNoSecret(wrong, WRONG_LOGIN.password)
        val missing = failureOf { bed.client(ProxyRoute.Socks5(proxy.endpoint)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.AUTH_REQUIRED, missing.proxyFailure())
        assertEquals(emptyList<String>(), proxy.targets)
        assertEquals(0, bed.resource.requests.get())
        assertEquals(emptyList<String>(), bed.dns.lookups)
    }

    @Test fun httpConnectSendsTheExactBasicHeader() {
        val proxy = bed.proxy(TestProxyKind.HTTP, LOGIN)
        assertEquals(RESOURCE_BODY, bed.client(ProxyRoute.Http(proxy.endpoint, LOGIN)).get(bed.resource.url()))
        assertEquals(listOf("Basic $LOGIN_BASIC"), proxy.logins)
        assertEquals(listOf(target), proxy.targets)
        assertEquals(emptyList<String>(), bed.dns.lookups)
    }

    @Test fun httpConnectReportsMissingAndRejectedLogins() {
        val proxy = bed.proxy(TestProxyKind.HTTP, LOGIN)
        val missing = failureOf { bed.client(ProxyRoute.Http(proxy.endpoint)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.AUTH_REQUIRED, missing.proxyFailure())
        val wrong = failureOf { bed.client(ProxyRoute.Http(proxy.endpoint, WRONG_LOGIN)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.AUTH_REJECTED, wrong.proxyFailure())
        assertNoSecret(wrong, WRONG_LOGIN.password, WRONG_LOGIN_BASIC)
        assertEquals(listOf("Basic $WRONG_LOGIN_BASIC"), proxy.logins)
        assertEquals(0, bed.resource.requests.get())
    }

    @Test fun serverThatIsNotAProxyIsReportedAsSuch() {
        val server = bed.proxy(TestProxyKind.NOT_A_PROXY)
        val socks = failureOf { bed.client(ProxyRoute.Socks5(server.endpoint)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.BAD_PROXY_RESPONSE, socks.proxyFailure())
        val http = failureOf { bed.client(ProxyRoute.Http(server.endpoint)).get(bed.resource.url()) }
        assertEquals(ProxyFailure.TARGET_UNREACHABLE, http.proxyFailure())
        assertEquals(0, bed.resource.requests.get())
    }

    @Test fun probeMeasuresTheHttpResponseThroughEachProtocol() =
        runBlocking {
            val socks = bed.proxy(TestProxyKind.SOCKS5)
            val http = bed.proxy(TestProxyKind.HTTP)
            val https = bed.proxy(TestProxyKind.HTTPS)
            val routes =
                listOf(
                    ProxyRoute.Socks5(socks.endpoint),
                    ProxyRoute.Http(http.endpoint),
                    ProxyRoute.Https(https.endpoint),
                )
            routes.forEach { route ->
                val builder =
                    OkHttpClient.Builder().sslSocketFactory(resourceTls.context.socketFactory, resourceTls.trustManager)
                val router = routerWith(route)
                val result = probeTarget(router, bed.resource.url(), builder, trusting(proxyTls))
                assertTrue("$route: $result", result is ProxyProbeResult.Response)
                assertEquals(200, (result as ProxyProbeResult.Response).code)
                assertTrue(result.elapsedMillis >= 0)
            }
            assertEquals(listOf(1, 1, 1), listOf(socks, http, https).map { it.targets.size })
            assertEquals(3, bed.resource.requests.get())
        }
}
