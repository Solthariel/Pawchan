package com.solthariel.pawchan.data.network.proxy

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * Creates [TunnelSocket]s. Only [createSocket] without arguments is used by OkHttp; the others
 * exist for completeness and resolve through [RoutedDns], so they cannot leak a lookup either.
 */
class RoutedSocketFactory(
    private val router: ScopedProxySelector,
    private val trust: ProxyTlsTrust = ProxyTlsTrust.system(),
) : SocketFactory() {
    private val dns = RoutedDns(router)

    override fun createSocket(): Socket = TunnelSocket(router, trust)

    override fun createSocket(
        host: String,
        port: Int,
    ): Socket = connected(dns.lookup(host).first(), port, null)

    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int,
    ): Socket = connected(dns.lookup(host).first(), port, InetSocketAddress(localHost, localPort))

    override fun createSocket(
        host: InetAddress,
        port: Int,
    ): Socket = connected(host, port, null)

    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int,
    ): Socket = connected(address, port, InetSocketAddress(localAddress, localPort))

    private fun connected(
        address: InetAddress,
        port: Int,
        local: InetSocketAddress?,
    ): Socket {
        val socket = createSocket()
        try {
            if (local != null) socket.bind(local)
            socket.connect(InetSocketAddress(address, port))
        } catch (failure: IOException) {
            socket.close()
            throw failure
        }
        return socket
    }
}
