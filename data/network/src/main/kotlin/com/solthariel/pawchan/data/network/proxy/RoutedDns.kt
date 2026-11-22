package com.solthariel.pawchan.data.network.proxy

import okhttp3.Dns
import java.net.InetAddress

/**
 * Name resolution that never leaks: a proxied resource is not looked up locally (the proxy does
 * that), and an unavailable one is refused instead of being resolved and reached directly.
 */
class RoutedDns(
    private val router: ScopedProxySelector,
    private val system: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val service = router.serviceForHost(hostname) ?: return system.lookup(hostname)
        val slot = router.slot(service)
        return when (slot.route) {
            ProxyRoute.Direct -> system.lookup(hostname).map { InetAddress.getByAddress(hostname, it.address) }
            ProxyRoute.Unavailable -> throw ProxyBlockedException("The proxy for this resource is not ready")
            is ProxyRoute.Proxied -> listOf(placeholderAddress(service, slot.epoch, hostname))
        }
    }
}
