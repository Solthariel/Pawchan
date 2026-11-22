package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ManualProxyAddressTest {
    @Test fun acceptsLocalNamedAndSchemeAddresses() {
        assertEquals(ProxyEndpoint("192.168.1.7", 1080), parseManualSocks5Address("socks5://192.168.1.7:1080"))
        assertEquals(ProxyEndpoint("proxy.example", 1081), parseManualHttpAddress("HTTP://Proxy.Example:1081"))
        assertEquals(ProxyEndpoint("localhost", 9050), parseManualHttpsAddress("localhost:9050"))
    }

    @Test fun rejectsCredentialsPathsAndInvalidAddresses() {
        listOf(
            "user:pass@host:1080",
            "http://host:1080",
            "host:1080/path",
            "host:1080?x",
            "host:1080#x",
            "1.2.3.999:1080",
            "host:0",
            "host:65536",
            "host name:1080",
            "host:1080\nother:1080",
            "host_name:80",
            "host:+80",
            "::1:1080",
            "[fe80::1%eth0]:1080",
        ).forEach { assertNull(it, parseManualSocks5Address(it)) }
    }

    @Test fun validatesNumericAndHostnameBoundaries() {
        listOf("0.0.0.0:1", "255.255.255.255:65535", "${"a".repeat(63)}:1")
            .forEach { requireNotNull(parseManualSocks5Address(it)) }
        listOf(
            "256.0.0.1:1",
            "01.0.0.1:1",
            "1.2.3:1",
            "1.2.3.4:0",
            "1.2.3.4:65536",
            "${"a".repeat(64)}:1",
            "-host:1",
            "host-:1",
            "host..example:1",
            "${"a".repeat(256)}:1",
        ).forEach { assertNull(it, parseManualSocks5Address(it)) }
    }

    @Test fun ipv6AndSeparateFields() {
        val endpoint = ProxyEndpoint("2001:db8::1", 443)
        assertEquals(endpoint, parseManualHttpsAddress("https://[2001:DB8::1]:443"))
        assertEquals(endpoint, proxyEndpointOf("2001:DB8::1", "443"))
        assertEquals(endpoint, proxyEndpointOf("[2001:DB8::1]", "443"))
        assertEquals(endpoint, parseProxyAddress(endpoint.authority))
        listOf("[:::1]:80", "[1::2::3]:80", "[gg::1]:80", "[::1]:0")
            .forEach { assertNull(it, parseProxyAddress(it)) }
        assertNull(proxyEndpointOf("host", "bad"))
    }

    @Test fun routesRespectModeAndDoNotExposeCredentials() {
        val credentials = ProxyCredentials("alice", "secret")
        val endpoint = ProxyEndpoint("host", 80)
        assertEquals(ProxyRoute.Socks5(endpoint, credentials), proxyRouteFor(ProxyMode.SOCKS5, "host:80", credentials))
        assertEquals(ProxyRoute.Http(endpoint, credentials), proxyRouteFor(ProxyMode.HTTP, "host:80", credentials))
        assertEquals(ProxyRoute.Https(endpoint, credentials), proxyRouteFor(ProxyMode.HTTPS, "host:80", credentials))
        assertNull(proxyRouteFor(ProxyMode.DIRECT, "host:80", credentials))
        assertNull(proxyRouteFor(ProxyMode.NEEDS_CONFIGURATION, "host:80", credentials))
        assertNull(proxyRouteFor(ProxyMode.HTTPS, "http://host:80", credentials))
        assertFalse(proxyRouteFor(ProxyMode.HTTP, "host:80", credentials).toString().contains("secret"))
    }
}
