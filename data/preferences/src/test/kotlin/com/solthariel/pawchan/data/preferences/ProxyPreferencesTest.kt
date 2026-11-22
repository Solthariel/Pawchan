package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyPreferencesTest {
    @Test fun missingCatalogDefaultsToDirect() {
        val settings = mutablePreferencesOf().readProxyPreferences()
        assertEquals(ProxyMode.DIRECT, settings.profile(ProxyService.DVACH).mode)
    }

    @Test fun corruptCatalogBlocksEveryService() {
        val stored = mutablePreferencesOf(stringPreferencesKey("proxy_catalog_v1") to "not valid")
        ProxyService.entries.forEach {
            assertEquals(ProxyMode.NEEDS_CONFIGURATION, stored.readProxyPreferences().profile(it).mode)
        }
        assertTrue(stored.readProxyPreferences().needsReset)
    }

    @Test fun missingOrUnreadableCredentialsBlockEveryService() {
        val proxy =
            SavedProxy(
                "id",
                "name",
                ProxyMode.HTTPS,
                "host:443",
                true,
                setOf(ProxyService.ARCHIVE),
                ProxyCredentials("alice", "secret"),
            )
        val encoded = encodeProxyCatalog(listOf(proxy))
        val key = stringPreferencesKey("proxy_catalog_v1")
        val secretKey = stringPreferencesKey("proxy_credentials_v1")
        val missing = mutablePreferencesOf(key to encoded.catalog)
        val unreadable = mutablePreferencesOf(key to encoded.catalog, secretKey to "AQ==")
        val failingSealer =
            object : ProxySecretSealer {
                override fun seal(plaintext: ByteArray): ByteArray = error("unavailable")

                override fun open(sealed: ByteArray): ByteArray = error("unavailable")
            }
        listOf(missing, unreadable).forEach { prefs ->
            val result = prefs.readProxyPreferences(failingSealer)
            assertTrue(result.needsReset)
            ProxyService.entries.forEach { assertEquals(ProxyMode.NEEDS_CONFIGURATION, result.profile(it).mode) }
        }
    }

    @Test fun previousCatalogVersionIsBlocked() {
        val old =
            java.util.Base64
                .getEncoder()
                .encodeToString(byteArrayOf(0, 0, 0, 1, 0, 0, 0, 0))
        val stored = mutablePreferencesOf(stringPreferencesKey("proxy_catalog_v1") to old)
        assertTrue(stored.readProxyPreferences().needsReset)
    }

    @Test fun oldSettingsAreNotMigratedAndRemainBlockedUntilExplicitReset() {
        val stored = mutablePreferencesOf(stringPreferencesKey("proxy_archive_mode") to "MANUAL")
        assertEquals(ProxyMode.NEEDS_CONFIGURATION, stored.readProxyPreferences().profile(ProxyService.ARCHIVE).mode)
        assertTrue(stored.readProxyPreferences().needsReset)
    }
}
