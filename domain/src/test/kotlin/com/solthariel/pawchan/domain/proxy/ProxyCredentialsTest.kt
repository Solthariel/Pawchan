package com.solthariel.pawchan.domain.proxy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyCredentialsTest {
    private fun proxy(
        mode: ProxyMode = ProxyMode.SOCKS5,
        credentials: ProxyCredentials? = null,
        services: Set<ProxyService> = setOf(ProxyService.DVACH),
        id: String = "a",
    ) = SavedProxy(id, "Home", mode, "127.0.0.1:1080", true, services, credentials)

    @Test
    fun toStringNeverShowsLoginOrPassword() {
        val credentials = ProxyCredentials("alice", "s3cret-pass")
        val shown =
            listOf(credentials, proxy(credentials = credentials), ProxyProfile(ProxyMode.HTTPS, "h:1", credentials))
        shown.forEach {
            assertFalse(it.toString().contains("alice"))
            assertFalse(it.toString().contains("s3cret-pass"))
        }
    }

    @Test
    fun catalogAcceptsProxiesWithAndWithoutCredentials() {
        val catalog =
            listOf(
                proxy(id = "a"),
                proxy(ProxyMode.HTTP, ProxyCredentials("bob", "pw"), setOf(ProxyService.ARCHIVE), "b"),
                proxy(ProxyMode.HTTPS, ProxyCredentials("bob", "pw"), setOf(ProxyService.GITHUB), "c"),
            )
        assertTrue(validProxyCatalog(catalog))
    }

    @Test
    fun catalogRejectsEmptyOrOverlongOrControlCredentials() {
        val bad =
            listOf(
                ProxyCredentials("", "pw"),
                ProxyCredentials("user", ""),
                ProxyCredentials("u".repeat(256), "pw"),
                ProxyCredentials("user", "p".repeat(256)),
                ProxyCredentials("us\ner", "pw"),
                ProxyCredentials("user", "p\u0000w"),
            )
        bad.forEach { assertFalse(it.toString(), validProxyCatalog(listOf(proxy(credentials = it)))) }
    }

    @Test
    fun overlongLimitCountsBytesNotCharacters() {
        val cyrillic = "ж".repeat(128)
        assertFalse(validProxyCatalog(listOf(proxy(credentials = ProxyCredentials(cyrillic, "pw")))))
        assertTrue(validProxyCatalog(listOf(proxy(credentials = ProxyCredentials("ж".repeat(127), "pw")))))
    }

    @Test
    fun httpBasicLoginCannotContainColonButSocksCan() {
        val withColon = ProxyCredentials("a:b", "pw")
        assertTrue(validProxyCatalog(listOf(proxy(ProxyMode.SOCKS5, withColon))))
        assertFalse(validProxyCatalog(listOf(proxy(ProxyMode.HTTP, withColon))))
        assertFalse(validProxyCatalog(listOf(proxy(ProxyMode.HTTPS, withColon))))
    }

    @Test
    fun onlyRealProxyModesAreStored() {
        assertFalse(validProxyCatalog(listOf(proxy(mode = ProxyMode.DIRECT))))
        assertFalse(validProxyCatalog(listOf(proxy(mode = ProxyMode.NEEDS_CONFIGURATION))))
    }

    @Test
    fun enabledProxiesCannotShareAResource() {
        val shared = setOf(ProxyService.DVACH)
        assertFalse(validProxyCatalog(listOf(proxy(id = "a", services = shared), proxy(id = "b", services = shared))))
    }

    @Test
    fun preferencesCarryCredentialsToEveryAssignedResource() {
        val credentials = ProxyCredentials("alice", "pw")
        val preferences =
            proxyPreferencesFromCatalog(
                listOf(proxy(ProxyMode.HTTPS, credentials, setOf(ProxyService.DVACH, ProxyService.ARCHIVE))),
            )
        assertEquals(credentials, preferences.profile(ProxyService.DVACH).credentials)
        assertEquals(ProxyMode.HTTPS, preferences.profile(ProxyService.ARCHIVE).mode)
        assertEquals(null, preferences.profile(ProxyService.GITHUB).credentials)
    }
}
