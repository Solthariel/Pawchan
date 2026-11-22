package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/** The current route of one resource and a counter that changes whenever the route does. */
data class ProxyRouteSlot(
    val route: ProxyRoute,
    val epoch: Int,
)

/**
 * Decides, per resource, whether requests go direct, through the user's proxy, or nowhere.
 *
 * As a plain [ProxySelector] it fails closed: every proxied or unavailable resource gets an
 * unusable endpoint, so a client that forgot to install the tunnel cannot reach the network
 * directly. Clients are configured with [viaProxyRouter], which opens the real tunnels.
 */
class ScopedProxySelector(
    private val defaultSelector: ProxySelector? = ProxySelector.getDefault(),
) : ProxySelector() {
    private val slots = AtomicReference<Map<ProxyService, ProxyRouteSlot>>(emptyMap())
    private val mirrorHosts = AtomicReference<Set<String>>(emptySet())

    fun registerMirror(host: String) {
        require(
            com.solthariel.pawchan.domain.settings
                .validMirrorHost(host),
        )
        mirrorHosts.updateAndGet { it + host }
    }

    fun serviceForHost(host: String?): ProxyService? {
        val normalized = host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return null
        return proxyServiceForHost(normalized)
            ?: if (mirrorHosts.get().any { normalized == it || normalized.endsWith(".$it") }) {
                ProxyService.DVACH
            } else {
                null
            }
    }

    fun update(
        service: ProxyService,
        route: ProxyRoute,
    ) {
        require(route !is ProxyRoute.Proxied || parseProxyAddress(route.endpoint.authority) == route.endpoint)
        slots.updateAndGet { current ->
            val old = current[service]
            if (old?.route == route) current else current + (service to ProxyRouteSlot(route, (old?.epoch ?: 0) + 1))
        }
    }

    fun route(service: ProxyService): ProxyRoute = slot(service).route

    /** Route and epoch are read together, so a connection can tell if the route changed under it. */
    fun slot(service: ProxyService): ProxyRouteSlot = slots.get()[service] ?: ProxyRouteSlot(ProxyRoute.Direct, 0)

    override fun select(uri: URI): List<Proxy> {
        val service = serviceForHost(uri.host) ?: return defaultRoute(uri)
        return if (route(service) == ProxyRoute.Direct) defaultRoute(uri) else listOf(BLOCKED_ROUTE)
    }

    /** What [viaProxyRouter] installs: tunnelled resources are opened by the socket factory, not by OkHttp. */
    internal fun tunnelSelector(): ProxySelector = TunnelProxySelector(this)

    private fun defaultRoute(uri: URI): List<Proxy> {
        val selected = defaultSelector?.select(uri)
        return selected?.takeIf { it.isNotEmpty() } ?: listOf(Proxy.NO_PROXY)
    }

    override fun connectFailed(
        uri: URI?,
        sa: SocketAddress?,
        ioe: IOException?,
    ) {
        if (uri != null && serviceForHost(uri.host)?.let(::route) == ProxyRoute.Direct) {
            defaultSelector?.connectFailed(uri, sa, ioe)
        }
    }
}

/**
 * Proxied and unavailable hosts are reported as "direct" to OkHttp. That is safe only because
 * [RoutedDns] refuses to resolve them and [RoutedSocketFactory] connects them only through the tunnel.
 */
internal class TunnelProxySelector(
    val router: ScopedProxySelector,
) : ProxySelector() {
    override fun select(uri: URI): List<Proxy> {
        val service = router.serviceForHost(uri.host)
        return if (service != null && router.route(service) != ProxyRoute.Direct) {
            listOf(Proxy.NO_PROXY)
        } else {
            router.select(uri)
        }
    }

    override fun connectFailed(
        uri: URI?,
        sa: SocketAddress?,
        ioe: IOException?,
    ) = router.connectFailed(uri, sa, ioe)
}

fun proxyServiceForHost(input: String?): ProxyService? {
    val host = input?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return null
    return when {
        matchesHost(host, DVACH_HOSTS) -> ProxyService.DVACH
        matchesHost(host, FOUR_CHAN_HOSTS) -> ProxyService.FOUR_CHAN
        matchesHost(host, ARCHIVE_HOSTS) -> ProxyService.ARCHIVE
        host == "voca.ro" || matchesHost(host, VOCAROO_HOSTS) -> ProxyService.VOCAROO
        matchesHost(host, GITHUB_HOSTS) -> ProxyService.GITHUB
        else -> null
    }
}

private fun matchesHost(
    host: String,
    domains: Set<String>,
): Boolean = domains.any { host == it || host.endsWith(".$it") }

private val FOUR_CHAN_HOSTS = setOf("4chan.org", "4channel.org", "4cdn.org")
private val ARCHIVE_HOSTS = setOf("arhivach.vc")
private val VOCAROO_HOSTS = setOf("vocaroo.com")
private val GITHUB_HOSTS = setOf("github.com", "githubusercontent.com")
private val DVACH_HOSTS = setOf("2ch.su", "2ch.org", "2ch.life", "2ch.hk", "2ch.pm")

// TCP port 0 cannot be a listening endpoint; fail closed while no proxy is ready.
private val BLOCKED_ROUTE = Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 0))
