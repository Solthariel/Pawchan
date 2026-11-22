package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyPreferences
import com.solthariel.pawchan.domain.proxy.ProxyProfile
import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.proxy.SavedProxy
import com.solthariel.pawchan.domain.proxy.proxyPreferencesFromCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Base64

/** Catalog and encrypted secrets are written together in one DataStore transaction. */
class ProxyPreferencesStore internal constructor(
    private val store: DataStore<Preferences>,
    private val sealer: ProxySecretSealer = KeystoreProxySealer(),
) {
    val preferences: Flow<ProxyPreferences> = store.data.map { it.readProxyPreferences(sealer) }

    suspend fun saveCatalog(catalog: List<SavedProxy>) {
        val encoded = encodeProxyCatalog(catalog)
        val sealed =
            encoded.secrets?.let { secret ->
                try {
                    Base64.getEncoder().encodeToString(sealer.seal(secret))
                } finally {
                    secret.fill(0)
                }
            }
        store.edit { prefs ->
            prefs[CATALOG_KEY] = encoded.catalog
            if (sealed == null) prefs.remove(SECRETS_KEY) else prefs[SECRETS_KEY] = sealed
            ProxyService.entries.forEach { service ->
                prefs.remove(stringPreferencesKey("proxy_${service.name.lowercase()}_mode"))
                prefs.remove(stringPreferencesKey("proxy_${service.name.lowercase()}_address"))
            }
        }
    }
}

internal fun Preferences.readProxyPreferences(sealer: ProxySecretSealer = KeystoreProxySealer()): ProxyPreferences {
    val encoded = this[CATALOG_KEY]
    val sealed = this[SECRETS_KEY]
    if (encoded == null) {
        val oldSettingsExist =
            sealed != null ||
                ProxyService.entries.any { service ->
                    this[stringPreferencesKey("proxy_${service.name.lowercase()}_mode")] != null ||
                        this[stringPreferencesKey("proxy_${service.name.lowercase()}_address")] != null
                }
        return if (oldSettingsExist) blockedPreferences() else ProxyPreferences()
    }
    return try {
        val secret = sealed?.let { sealer.open(Base64.getDecoder().decode(it)) }
        val catalog =
            try {
                decodeProxyCatalog(encoded, secret)
            } finally {
                secret?.fill(0)
            }
        if (catalog == null || (sealed != null && catalog.none { it.credentials != null })) {
            blockedPreferences()
        } else {
            proxyPreferencesFromCatalog(catalog)
        }
    } catch (_: Exception) {
        blockedPreferences()
    }
}

private fun blockedPreferences() =
    ProxyPreferences(
        profiles = ProxyService.entries.associateWith { ProxyProfile(ProxyMode.NEEDS_CONFIGURATION) },
        needsReset = true,
    )

private val CATALOG_KEY = stringPreferencesKey("proxy_catalog_v1")
private val SECRETS_KEY = stringPreferencesKey("proxy_credentials_v1")
