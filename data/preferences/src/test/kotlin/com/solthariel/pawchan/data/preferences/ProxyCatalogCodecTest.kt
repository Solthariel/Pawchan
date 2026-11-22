package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ProxyCatalogCodecTest {
    private val proxy =
        SavedProxy(
            "id",
            "Прокси",
            ProxyMode.SOCKS5,
            "example.org:1080",
            true,
            setOf(ProxyService.DVACH, ProxyService.ARCHIVE),
        )

    @Test fun roundTripRetainsEnabledAndDisabledProfiles() {
        val list = listOf(proxy, proxy.copy(id = "two", name = "Другой", enabled = false))
        assertEquals(list, encodeProxyCatalog(list).let { decodeProxyCatalog(it.catalog, it.secrets) })
    }

    @Test fun maximumCatalogWithMultibyteTextCanBeReadBack() {
        val profiles =
            (0 until 100).map { index ->
                proxy.copy(
                    id = index.toString().padStart(80, 'я'),
                    name = "я".repeat(100),
                    address = "я".repeat(255),
                    enabled = false,
                )
            }
        assertEquals(profiles, encodeProxyCatalog(profiles).let { decodeProxyCatalog(it.catalog, it.secrets) })
    }

    @Test fun credentialsAreSeparateAndRequiredForDecoding() {
        val profile = proxy.copy(credentials = ProxyCredentials("user", "secret"))
        val encoded = encodeProxyCatalog(listOf(profile))
        assertEquals(listOf(profile), decodeProxyCatalog(encoded.catalog, encoded.secrets))
        assertNull(decodeProxyCatalog(encoded.catalog))
        assertEquals(
            false,
            String(
                java.util.Base64
                    .getDecoder()
                    .decode(encoded.catalog),
            ).contains("secret"),
        )
        assertNull(decodeProxyCatalog(encoded.catalog, byteArrayOf(1)))
    }

    @Test fun truncatedOrUnknownDataIsRejected() {
        val encoded = encodeProxyCatalog(listOf(proxy)).catalog
        assertNull(decodeProxyCatalog(encoded.dropLast(8)))
        assertNull(decodeProxyCatalog("!!!!!"))
    }

    @Test fun overlappingAssignmentsAndCredentialsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            encodeProxyCatalog(listOf(proxy, proxy.copy(id = "two")))
        }
        assertThrows(IllegalArgumentException::class.java) {
            encodeProxyCatalog(listOf(proxy.copy(address = "user:pass@example.org:1080")))
        }
    }
}
