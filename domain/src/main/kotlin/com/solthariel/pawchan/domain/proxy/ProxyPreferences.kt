package com.solthariel.pawchan.domain.proxy

/** Per-service configuration; proxy selection is opt-in. */
enum class ProxyService { DVACH, FOUR_CHAN, ARCHIVE, VOCAROO, GITHUB }

/** The proxy protocol is part of the profile: SOCKS5, HTTP CONNECT or HTTP CONNECT over TLS. */
enum class ProxyMode {
    DIRECT,
    NEEDS_CONFIGURATION,
    SOCKS5,
    HTTP,
    HTTPS,
    ;

    /** True for the modes that really reach a configured proxy server. */
    val isProxy: Boolean get() = this == SOCKS5 || this == HTTP || this == HTTPS
}

data class ProxyProfile(
    val mode: ProxyMode = ProxyMode.DIRECT,
    val manualAddress: String = "",
    val credentials: ProxyCredentials? = null,
)

data class ProxyPreferences(
    val profiles: Map<ProxyService, ProxyProfile> = emptyMap(),
    val savedProxies: List<SavedProxy> = emptyList(),
    val needsReset: Boolean = false,
) {
    fun profile(service: ProxyService): ProxyProfile = profiles[service] ?: ProxyProfile()
}
