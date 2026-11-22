package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.InetAddress

class RoutedDnsTest {
    private val system = DirectDnsSpy()
    private val socks = ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1080))

    @Test fun proxiedResourceIsNeverResolvedLocally() {
        val router = routerWith(socks)
        val address = RoutedDns(router, system).lookup("i.arhivach.vc").single()
        val epoch = router.slot(ProxyService.ARCHIVE).epoch
        val expected = ProxyPlaceholder(ProxyService.ARCHIVE, epoch, hostDigest("i.arhivach.vc"))
        assertEquals(expected, parsePlaceholder(address))
        assertEquals("i.arhivach.vc", address.hostName)
        assertEquals(emptyList<String>(), system.lookups)
    }

    @Test fun unavailableResourceIsRefusedWithoutLookup() {
        val dns = RoutedDns(routerWith(ProxyRoute.Unavailable), system)
        val failure = assertThrows(ProxyBlockedException::class.java) { dns.lookup("arhivach.vc") }
        assertEquals(ProxyFailure.NOT_READY, failure.proxyFailure())
        assertEquals(emptyList<String>(), system.lookups)
    }

    @Test fun directAndUnknownHostsUseTheSystemResolver() {
        val dns = RoutedDns(ScopedProxySelector(null), system)
        val direct = dns.lookup("arhivach.vc").single()
        assertEquals("arhivach.vc", direct.hostName)
        assertArrayEquals(InetAddress.getLoopbackAddress().address, direct.address)
        assertNull(parsePlaceholder(direct))
        RoutedDns(routerWith(socks), system).lookup("example.com")
        assertEquals(listOf("arhivach.vc", "example.com"), system.lookups)
    }

    @Test fun placeholderCarriesServiceEpochAndHost() {
        ProxyService.entries.forEach { service ->
            val address = placeholderAddress(service, 7, "Host.Example")
            assertEquals(ProxyPlaceholder(service, 7, hostDigest("host.example")), parsePlaceholder(address))
            assertEquals("unique-local range", 0xFD.toByte(), address.address[0])
        }
        val first = placeholderAddress(ProxyService.ARCHIVE, 1, "arhivach.vc")
        assertNotEquals(first, placeholderAddress(ProxyService.ARCHIVE, 2, "arhivach.vc"))
        assertNotEquals(first, placeholderAddress(ProxyService.ARCHIVE, 1, "i.arhivach.vc"))
        assertNotEquals(first, placeholderAddress(ProxyService.VOCAROO, 1, "arhivach.vc"))
    }

    @Test fun ordinaryAddressesAreNotPlaceholders() {
        listOf("127.0.0.1", "::1", "2001:db8::1", "fd00::1").forEach {
            assertNull(it, parsePlaceholder(InetAddress.getByName(it)))
        }
        val unknownService = byteArrayOf(0xFD.toByte(), 0x50, 0x43, 0x7F) + ByteArray(12)
        assertNull(parsePlaceholder(InetAddress.getByAddress(unknownService)))
        assertNull(parsePlaceholder(null))
    }

    @Test fun routeChangeIssuesANewPlaceholder() {
        val router = routerWith(socks)
        val dns = RoutedDns(router, system)
        val before = requireNotNull(parsePlaceholder(dns.lookup("arhivach.vc").single()))
        router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1081)))
        val after = requireNotNull(parsePlaceholder(dns.lookup("arhivach.vc").single()))
        assertEquals(before.epoch + 1, after.epoch)
        router.update(ProxyService.ARCHIVE, ProxyRoute.Socks5(ProxyEndpoint("127.0.0.1", 1081)))
        assertEquals(after, parsePlaceholder(dns.lookup("arhivach.vc").single()))
    }
}
