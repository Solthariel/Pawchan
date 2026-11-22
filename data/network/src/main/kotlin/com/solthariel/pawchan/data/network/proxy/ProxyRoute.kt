package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyProfile

/** A site identity is independent of a particular mirror or CDN host. */
sealed interface ProxyRoute {
    data object Direct : ProxyRoute

    /** The proxy is not configured or available: never fall back to a direct request. */
    data object Unavailable : ProxyRoute

    /** A proxy server the user chose. The tunnel to the resource is always opened through it. */
    sealed interface Proxied : ProxyRoute {
        val endpoint: ProxyEndpoint
        val credentials: ProxyCredentials?
    }

    data class Socks5(
        override val endpoint: ProxyEndpoint,
        override val credentials: ProxyCredentials? = null,
    ) : Proxied

    /** HTTP CONNECT. The connection to the proxy itself is not encrypted. */
    data class Http(
        override val endpoint: ProxyEndpoint,
        override val credentials: ProxyCredentials? = null,
    ) : Proxied

    /** HTTP CONNECT inside TLS: the proxy certificate and host name are verified. */
    data class Https(
        override val endpoint: ProxyEndpoint,
        override val credentials: ProxyCredentials? = null,
    ) : Proxied
}

/** The route of a stored proxy, or null when the address does not describe a usable proxy. */
fun proxyRouteFor(
    mode: ProxyMode,
    address: String,
    credentials: ProxyCredentials?,
): ProxyRoute.Proxied? =
    when (mode) {
        ProxyMode.SOCKS5 -> parseManualSocks5Address(address)?.let { ProxyRoute.Socks5(it, credentials) }
        ProxyMode.HTTP -> parseManualHttpAddress(address)?.let { ProxyRoute.Http(it, credentials) }
        ProxyMode.HTTPS -> parseManualHttpsAddress(address)?.let { ProxyRoute.Https(it, credentials) }
        ProxyMode.DIRECT, ProxyMode.NEEDS_CONFIGURATION -> null
    }

fun proxyRouteFor(profile: ProxyProfile): ProxyRoute.Proxied? {
    val (mode, address, credentials) = profile
    return proxyRouteFor(mode, address, credentials)
}
