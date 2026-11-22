package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyProfile
import com.solthariel.pawchan.domain.proxy.ProxyService
import org.junit.Assert.assertEquals
import org.junit.Test

class PrepareProxyRoutesTest {
    @Test fun restoredHttpProxyUsesHttpAndInvalidAddressFailsClosed() {
        val router = ScopedProxySelector(null)
        val prefs =
            ProxyPreferences(
                mapOf(
                    ProxyService.ARCHIVE to ProxyProfile(ProxyMode.HTTP, "http://127.0.0.1:8080"),
                    ProxyService.GITHUB to ProxyProfile(ProxyMode.HTTP, "https://example.com:443"),
                ),
            )
        prepareProxyRoutes(router, prefs, setOf(ProxyService.ARCHIVE, ProxyService.GITHUB))
        assertEquals(ProxyRoute.Http(ProxyEndpoint("127.0.0.1", 8080)), router.route(ProxyService.ARCHIVE))
        assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.GITHUB))
    }

    @Test fun manualAndUnavailablePoliciesRemainIndependent() {
        val router = ScopedProxySelector()
        val prefs =
            ProxyPreferences(
                mapOf(
                    ProxyService.ARCHIVE to ProxyProfile(ProxyMode.NEEDS_CONFIGURATION),
                    ProxyService.VOCAROO to ProxyProfile(ProxyMode.SOCKS5, "1.1.1.1:1234"),
                    ProxyService.GITHUB to ProxyProfile(ProxyMode.SOCKS5, "bad"),
                ),
            )
        prepareProxyRoutes(router, prefs, ProxyService.entries.toSet())
        assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.ARCHIVE))
        assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.GITHUB))
        assertEquals(ProxyRoute.Socks5(ProxyEndpoint("1.1.1.1", 1234)), router.route(ProxyService.VOCAROO))
        assertEquals(ProxyRoute.Direct, router.route(ProxyService.DVACH))
    }

    @Test fun previouslyOpenRouteIsBlockedForUnconfiguredPolicy() {
        val router = ScopedProxySelector()
        router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(ProxyEndpoint("1.1.1.1", 1080)))
        val prefs = ProxyPreferences(mapOf(ProxyService.ARCHIVE to ProxyProfile(ProxyMode.NEEDS_CONFIGURATION)))
        prepareProxyRoutes(router, prefs, setOf(ProxyService.ARCHIVE))
        assertEquals(ProxyRoute.Unavailable, router.route(ProxyService.ARCHIVE))
    }
}
