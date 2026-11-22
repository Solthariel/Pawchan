package com.solthariel.pawchan.domain.proxy

/**
 * A user-defined proxy may serve several resources; enabled assignments must not overlap.
 * [address] is the canonical `host:port` (IPv6 hosts are written as `[::1]:1080`).
 */
data class SavedProxy(
    val id: String,
    val name: String,
    val mode: ProxyMode,
    val address: String,
    val enabled: Boolean,
    val services: Set<ProxyService>,
    val credentials: ProxyCredentials? = null,
)

fun validProxyCatalog(catalog: List<SavedProxy>): Boolean {
    val sizeValid = catalog.size <= MAX_PROFILES && catalog.map { it.id }.toSet().size == catalog.size
    val fieldsValid = catalog.all(::validProxyFields)
    val assigned = catalog.filter { it.enabled }.flatMap { it.services }
    return sizeValid && fieldsValid && assigned.size == assigned.toSet().size
}

private fun validProxyFields(proxy: SavedProxy): Boolean {
    val idValid = proxy.id.isNotBlank() && proxy.id.length <= MAX_ID_LENGTH
    val nameValid = proxy.name.isNotBlank() && proxy.name.length <= MAX_NAME_LENGTH
    val addressValid = proxy.address.length <= MAX_ADDRESS_LENGTH && '@' !in proxy.address
    val credentialsValid = proxy.credentials?.let { validProxyCredentials(it, proxy.mode) } ?: true
    return idValid && nameValid && addressValid && proxy.mode.isProxy && credentialsValid
}

fun proxyPreferencesFromCatalog(catalog: List<SavedProxy>): ProxyPreferences {
    require(validProxyCatalog(catalog))
    return ProxyPreferences(
        profiles =
            catalog
                .filter { it.enabled }
                .flatMap { proxy ->
                    proxy.services.map { it to ProxyProfile(proxy.mode, proxy.address, proxy.credentials) }
                }.toMap(),
        savedProxies = catalog,
    )
}

private const val MAX_PROFILES = 100
private const val MAX_ID_LENGTH = 80
private const val MAX_NAME_LENGTH = 100
private const val MAX_ADDRESS_LENGTH = 255
