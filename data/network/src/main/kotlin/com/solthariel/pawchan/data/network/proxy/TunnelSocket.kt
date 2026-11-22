package com.solthariel.pawchan.data.network.proxy

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketTimeoutException

/**
 * A socket that decides at connect time how to reach its target. A placeholder address from
 * [RoutedDns] means "open a tunnel through the proxy selected for this resource"; any other address
 * is a direct connection, allowed only while the resource's route really is direct. There is no
 * path that connects a proxied or unavailable resource without its proxy.
 */
internal class TunnelSocket(
    private val router: ScopedProxySelector,
    private val trust: ProxyTlsTrust,
) : Socket() {
    @Volatile private var tunnelLeg: Socket? = null

    @Volatile private var relay: HttpsProxyRelay? = null

    override fun connect(
        endpoint: SocketAddress,
        timeout: Int,
    ) {
        val target = endpoint as? InetSocketAddress ?: throw IllegalArgumentException("Unsupported address type")
        val placeholder = parsePlaceholder(target.address)
        if (placeholder == null) {
            requireDirectAllowed(target.hostString)
            super.connect(endpoint, timeout)
        } else {
            val route = currentRoute(placeholder, target.hostString)
            try {
                if (route is ProxyRoute.Https) {
                    connectThroughHttps(route, target, timeout)
                } else {
                    connectThroughPlain(route, target, timeout)
                }
            } catch (failure: IOException) {
                close()
                throw failure
            }
        }
    }

    private fun requireDirectAllowed(host: String) {
        val service = router.serviceForHost(host) ?: return
        if (router.route(service) != ProxyRoute.Direct) {
            throw ProxyTunnelException(ProxyFailure.NOT_READY, "The proxy for this resource is not ready")
        }
    }

    private fun currentRoute(
        placeholder: ProxyPlaceholder,
        host: String,
    ): ProxyRoute.Proxied {
        val slot = router.slot(placeholder.service)
        val sameRoute =
            slot.epoch == placeholder.epoch && router.serviceForHost(host) == placeholder.service &&
                hostDigest(host) == placeholder.hostDigest
        return (slot.route as? ProxyRoute.Proxied)?.takeIf { sameRoute }
            ?: throw ProxyTunnelException(ProxyFailure.NOT_READY, "The proxy route changed while connecting")
    }

    private fun connectThroughPlain(
        route: ProxyRoute.Proxied,
        target: InetSocketAddress,
        timeout: Int,
    ) {
        // The proxy leg is this very socket; super.connect never re-enters the routing above.
        connectToProxy(route.endpoint) { super.connect(it, timeout) }
        val previous = soTimeout
        soTimeout = handshakeTimeout(timeout)
        runHandshake { socks5OrHttp(route, target, getInputStream(), getOutputStream()) }
        soTimeout = previous
    }

    private fun connectThroughHttps(
        route: ProxyRoute.Https,
        target: InetSocketAddress,
        timeout: Int,
    ) {
        val plain = Socket()
        tunnelLeg = plain
        connectToProxy(route.endpoint) { plain.connect(it, timeout) }
        plain.soTimeout = handshakeTimeout(timeout)
        val tls = runHandshake { trust.connect(plain, route.endpoint) }
        tunnelLeg = tls
        runHandshake {
            httpConnect(tls.getInputStream(), tls.getOutputStream(), target.hostString, target.port, route.credentials)
        }
        tls.soTimeout = 0
        relay =
            HttpsProxyRelay.open(tls, timeout) { local ->
                super.connect(local, timeout)
                localPort
            }
    }

    private fun socks5OrHttp(
        route: ProxyRoute.Proxied,
        target: InetSocketAddress,
        input: InputStream,
        output: OutputStream,
    ) {
        if (route is ProxyRoute.Socks5) {
            socks5Connect(input, output, target.hostString, target.port, route.credentials)
        } else {
            httpConnect(input, output, target.hostString, target.port, route.credentials)
        }
    }

    override fun close() {
        // Closing aborts a handshake that is still waiting for the proxy: this is how a call is cancelled.
        closeQuietly(tunnelLeg)
        relay?.close()
        super.close()
    }
}

/** Only the proxy server itself is resolved on this device; [connect] opens the plain TCP leg to it. */
private inline fun connectToProxy(
    endpoint: ProxyEndpoint,
    connect: (InetSocketAddress) -> Unit,
) {
    try {
        connect(InetSocketAddress(endpoint.host, endpoint.port))
    } catch (failure: SocketTimeoutException) {
        throw ProxyTunnelException(ProxyFailure.TIMEOUT, "The proxy did not accept the connection in time", failure)
    } catch (failure: IOException) {
        throw ProxyTunnelException(ProxyFailure.PROXY_UNREACHABLE, "Could not connect to the proxy", failure)
    }
}

/** Failures of the handshake become [ProxyTunnelException]s; a timeout and a dropped connection are told apart. */
private inline fun <T> runHandshake(handshake: () -> T): T =
    try {
        handshake()
    } catch (failure: IOException) {
        throw handshakeFailure(failure)
    }

private fun handshakeFailure(failure: IOException): ProxyTunnelException =
    when (failure) {
        is ProxyTunnelException -> {
            failure
        }

        is SocketTimeoutException -> {
            ProxyTunnelException(ProxyFailure.TIMEOUT, "The proxy did not answer in time", failure)
        }

        else -> {
            ProxyTunnelException(ProxyFailure.BAD_PROXY_RESPONSE, "The connection to the proxy broke", failure)
        }
    }

private fun handshakeTimeout(connectTimeout: Int): Int {
    val fallback = DEFAULT_HANDSHAKE_TIMEOUT_MILLIS
    return if (connectTimeout > 0) connectTimeout else fallback
}

private const val DEFAULT_HANDSHAKE_TIMEOUT_MILLIS = 30_000
