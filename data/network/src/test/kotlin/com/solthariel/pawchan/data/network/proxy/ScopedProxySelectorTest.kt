package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ScopedProxySelectorTest {
    private val blocked = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 0))

    @Test fun routesServicesIndependentlyIncludingTheirMediaHosts() {
        val selector = ScopedProxySelector()
        val archive = ProxyRoute.Socks5(ProxyEndpoint("8.8.8.8", 1080))
        val vocaroo = ProxyRoute.Http(ProxyEndpoint("1.1.1.1", 9000))
        selector.update(ProxyService.ARCHIVE, archive)
        selector.update(ProxyService.VOCAROO, vocaroo)
        assertEquals(archive, selector.serviceForHost("i.arhivach.vc")?.let(selector::route))
        assertEquals(vocaroo, selector.serviceForHost("media.vocaroo.com")?.let(selector::route))
        assertEquals(ProxyRoute.Direct, selector.route(ProxyService.FOUR_CHAN))
        assertEquals(Proxy.NO_PROXY, selector.select(URI("https://a.4cdn.org/boards.json")).single())
        assertEquals(Proxy.NO_PROXY, selector.select(URI("https://notarhivach.vc/")).single())
        assertEquals(ProxyService.GITHUB, proxyServiceForHost("objects.githubusercontent.com"))
        assertEquals(ProxyService.DVACH, proxyServiceForHost("2ch.hk"))
    }

    @Test fun unavailableProxyFailsBeforeDnsAndNeverUsesDirectRoute() {
        val selector = ScopedProxySelector()
        selector.update(ProxyService.ARCHIVE, ProxyRoute.Unavailable)
        val dnsCalled = AtomicBoolean(false)
        val client =
            OkHttpClient
                .Builder()
                .proxySelector(selector)
                .retryOnConnectionFailure(false)
                .callTimeout(2, TimeUnit.SECONDS)
                .dns(
                    Dns {
                        dnsCalled.set(true)
                        emptyList()
                    },
                ).build()
        val request = Request.Builder().url("https://arhivach.vc/").build()
        val failure = runCatching { client.newCall(request).execute() }.exceptionOrNull()
        assertEquals(blocked, selector.select(URI("https://arhivach.vc/")).single())
        assertTrue("Must fail rather than connect directly: $failure", failure is IOException)
        assertFalse("Must not resolve the target locally", dnsCalled.get())
    }

    @Test fun plainSelectorNeverHandsOutTheProxyItself() {
        val selector = ScopedProxySelector()
        val target = URI("https://boards.4channel.org/g/")
        selector.update(ProxyService.FOUR_CHAN, ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080)))
        assertEquals(blocked, selector.select(target).single())
        selector.update(ProxyService.FOUR_CHAN, ProxyRoute.Https(ProxyEndpoint("localhost", 443)))
        assertEquals(blocked, selector.select(target).single())
        selector.update(ProxyService.FOUR_CHAN, ProxyRoute.Unavailable)
        assertEquals(blocked, selector.select(target).single())
    }

    @Test fun tunnelSelectorLeavesProxiedHostsToTheTunnel() {
        val router = ScopedProxySelector(null)
        router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080)))
        router.update(ProxyService.VOCAROO, ProxyRoute.Unavailable)
        val tunnel = router.tunnelSelector()
        assertEquals(Proxy.NO_PROXY, tunnel.select(URI("https://arhivach.vc/")).single())
        assertEquals(Proxy.NO_PROXY, tunnel.select(URI("https://vocaroo.com/")).single())
        assertEquals(Proxy.NO_PROXY, tunnel.select(URI("https://2ch.su/")).single())
        val client = OkHttpClient.Builder().viaProxyRouter(router).build()
        assertSame(router, client.proxyRouter())
        assertSame(router, client.newBuilder().build().proxyRouter())
        assertNull(
            OkHttpClient
                .Builder()
                .proxySelector(router)
                .build()
                .proxyRouter(),
        )
    }

    @Test fun epochChangesOnlyWhenTheRouteDoes() {
        val router = ScopedProxySelector(null)
        val route = ProxyRoute.Socks5(requireNotNull(parseManualSocks5Address("127.0.0.1:1080")))
        assertEquals(ProxyRouteSlot(ProxyRoute.Direct, 0), router.slot(ProxyService.ARCHIVE))
        router.update(ProxyService.ARCHIVE, route)
        router.update(ProxyService.ARCHIVE, route.copy())
        assertEquals(ProxyRouteSlot(route, 1), router.slot(ProxyService.ARCHIVE))
        router.update(ProxyService.ARCHIVE, ProxyRoute.Unavailable)
        router.update(ProxyService.ARCHIVE, route)
        assertEquals(ProxyRouteSlot(route, 3), router.slot(ProxyService.ARCHIVE))
        assertEquals(0, router.slot(ProxyService.DVACH).epoch)
    }

    @Test fun rejectsEndpointsThatAreNotCanonical() {
        val router = ScopedProxySelector(null)
        listOf(ProxyEndpoint("Bad Host", 1080), ProxyEndpoint("proxy.example", 0), ProxyEndpoint("[::1]", 1080))
            .forEach { endpoint ->
                assertThrows(IllegalArgumentException::class.java) {
                    router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(endpoint))
                }
            }
        router.update(ProxyService.ARCHIVE, ProxyRoute.Https(ProxyEndpoint("::1", 1080)))
        assertEquals(ProxyRoute.Https(ProxyEndpoint("::1", 1080)), router.route(ProxyService.ARCHIVE))
    }

    @Test fun customMirrorIsRoutedAsDvachAndNotMatchedAsAnotherSite() {
        val router = ScopedProxySelector()
        router.registerMirror("mirror.example")
        router.update(ProxyService.DVACH, ProxyRoute.Unavailable)
        assertEquals(ProxyService.DVACH, router.serviceForHost("mirror.example"))
        val address = router.select(URI("https://mirror.example/index.json")).single().address() as InetSocketAddress
        assertEquals(0, address.port)
    }

    @Test fun knownSitesCannotBeReclassifiedAsCustomMirrors() {
        val selector = ScopedProxySelector()
        selector.registerMirror("github.com")
        assertEquals(ProxyService.GITHUB, selector.serviceForHost("github.com"))
    }

    @Test fun hostMatchingRespectsDomainBoundariesAndNormalization() {
        val cases =
            mapOf(
                "2CH.SU." to ProxyService.DVACH,
                "img.2ch.hk" to ProxyService.DVACH,
                "a.4chan.org" to ProxyService.FOUR_CHAN,
                "boards.4channel.org" to ProxyService.FOUR_CHAN,
                "a.4cdn.org" to ProxyService.FOUR_CHAN,
                "arhivach.vc" to ProxyService.ARCHIVE,
                "voca.ro" to ProxyService.VOCAROO,
                "media.vocaroo.com" to ProxyService.VOCAROO,
                "api.github.com" to ProxyService.GITHUB,
                "objects.githubusercontent.com" to ProxyService.GITHUB,
            )
        cases.forEach { (host, service) -> assertEquals(host, service, proxyServiceForHost(host)) }
        listOf(
            "not2ch.su",
            "2ch.su.evil",
            "not4cdn.org",
            "not4channel.org",
            "4channel.org.evil",
            "sub.voca.ro",
            "github.com.evil",
        ).forEach { assertEquals(it, null, proxyServiceForHost(it)) }
        assertEquals(null, proxyServiceForHost(null))
        val selector = ScopedProxySelector()
        selector.registerMirror("mirror.example")
        selector.registerMirror("github.com")
        assertEquals(ProxyService.DVACH, selector.serviceForHost("SUB.MIRROR.EXAMPLE."))
        assertEquals(null, selector.serviceForHost("notmirror.example"))
        assertEquals(ProxyService.GITHUB, selector.serviceForHost("api.github.com"))
    }
}
