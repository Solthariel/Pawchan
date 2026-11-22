package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import okhttp3.Address
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Route
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy

class ProxyRedirectGuardTest {
    private val url = "https://arhivach.vc/".toHttpUrl()
    private val loopback = InetAddress.getLoopbackAddress()

    @Test fun protectedServiceCannotRedirectToAnUnknownHostOrToHttp() {
        val router = ScopedProxySelector()
        router.update(ProxyService.ARCHIVE, ProxyRoute.Unavailable)
        val from = "https://arhivach.vc/thread/1".toHttpUrl()
        assertTrue(proxyRedirectAllowed(router, from, "https://i.arhivach.vc/a".toHttpUrl()))
        assertFalse(proxyRedirectAllowed(router, from, "https://elsewhere.example/a".toHttpUrl()))
        assertFalse(proxyRedirectAllowed(router, from, "http://arhivach.vc/a".toHttpUrl()))
        assertFalse(proxyRedirectAllowed(router, from, null))
    }

    @Test fun directModeKeepsExistingRedirectBehavior() {
        val router = ScopedProxySelector()
        assertTrue(
            proxyRedirectAllowed(router, "https://arhivach.vc/".toHttpUrl(), "https://elsewhere.example/".toHttpUrl()),
        )
    }

    @Test fun proxiedRouteAcceptsOnlyTheTunnelOfTheCurrentRoute() {
        val router = routerWith(ProxyRoute.Http(ProxyEndpoint("127.0.0.1", 8080)))
        val epoch = router.slot(ProxyService.ARCHIVE).epoch
        val current = routeTo(placeholderAddress(ProxyService.ARCHIVE, epoch, "arhivach.vc"))
        assertTrue(proxyConnectionAllowed(router, url, current))
        assertFalse(proxyConnectionAllowed(router, "http://arhivach.vc/".toHttpUrl(), current))
        listOf(
            placeholderAddress(ProxyService.ARCHIVE, epoch - 1, "arhivach.vc"),
            placeholderAddress(ProxyService.ARCHIVE, epoch, "i.arhivach.vc"),
            placeholderAddress(ProxyService.VOCAROO, epoch, "arhivach.vc"),
            loopback,
        ).forEach { assertFalse("$it", proxyConnectionAllowed(router, url, routeTo(it))) }
        assertFalse(proxyConnectionAllowed(router, url, null))
    }

    @Test fun directAndUnavailableRoutesNeverReuseATunnel() {
        val router = ScopedProxySelector(null)
        val tunnel = routeTo(placeholderAddress(ProxyService.ARCHIVE, 0, "arhivach.vc"))
        assertTrue(proxyConnectionAllowed(router, url, routeTo(loopback)))
        assertFalse(proxyConnectionAllowed(router, url, tunnel))
        router.update(ProxyService.ARCHIVE, ProxyRoute.Unavailable)
        assertFalse(proxyConnectionAllowed(router, url, routeTo(loopback)))
        assertFalse(proxyConnectionAllowed(router, url, tunnel))
        assertTrue(proxyConnectionAllowed(router, "https://example.com/".toHttpUrl(), routeTo(loopback)))
    }
}

private fun routeTo(address: InetAddress): Route {
    val client = OkHttpClient()
    val target =
        Address(
            "arhivach.vc",
            443,
            client.dns,
            client.socketFactory,
            client.sslSocketFactory,
            client.hostnameVerifier,
            client.certificatePinner,
            client.proxyAuthenticator,
            null,
            client.protocols,
            client.connectionSpecs,
            client.proxySelector,
        )
    return Route(target, Proxy.NO_PROXY, InetSocketAddress(address, 443))
}
