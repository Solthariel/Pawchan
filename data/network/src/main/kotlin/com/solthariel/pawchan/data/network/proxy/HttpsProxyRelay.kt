package com.solthariel.pawchan.data.network.proxy

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * TLS to the resource cannot be stacked on top of an [javax.net.ssl.SSLSocket] to the proxy on every
 * platform TLS provider, so the proxy leg is bridged through a private loopback connection: the HTTP
 * client talks TLS to a plain local socket and this relay carries the bytes over the (already
 * established, authenticated and verified) TLS tunnel to the proxy.
 *
 * The loopback listener accepts exactly one connection, the one from [clientPort], then closes.
 */
internal class HttpsProxyRelay private constructor(
    private val tunnel: Socket,
    private val local: Socket,
) : AutoCloseable {
    private val closed = AtomicBoolean(false)

    private fun start() {
        pumpThread("pawchan-proxy-up") { pump(local.getInputStream(), tunnel.getOutputStream()) { close() } }
        pumpThread("pawchan-proxy-down") {
            pump(tunnel.getInputStream(), local.getOutputStream()) {
                // The resource closed its side: tell the client, then release everything.
                try {
                    local.shutdownOutput()
                } catch (_: IOException) {
                    close()
                }
            }
        }
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            closeQuietly(tunnel)
            closeQuietly(local)
        }
    }

    companion object {
        /**
         * Opens the loopback pair. [connectClient] connects the client's socket to the given local
         * address and returns that socket's local port, so a foreign process cannot take its place.
         */
        fun open(
            tunnel: Socket,
            timeoutMillis: Int,
            connectClient: (InetSocketAddress) -> Int,
        ): HttpsProxyRelay {
            ServerSocket(0, 1, InetAddress.getLoopbackAddress()).use { server ->
                val clientPort = connectClient(InetSocketAddress(server.inetAddress, server.localPort))
                server.soTimeout = timeoutMillis.takeIf { it > 0 } ?: ACCEPT_TIMEOUT_MILLIS
                val local = acceptClient(server, clientPort)
                return HttpsProxyRelay(tunnel, local).also { it.start() }
            }
        }

        private fun acceptClient(
            server: ServerSocket,
            clientPort: Int,
        ): Socket {
            repeat(MAX_FOREIGN_CONNECTIONS) {
                val candidate =
                    try {
                        server.accept()
                    } catch (timeout: SocketTimeoutException) {
                        throw ProxyTunnelException(ProxyFailure.OTHER, "The local proxy bridge did not start", timeout)
                    }
                if (candidate.port == clientPort) return candidate
                closeQuietly(candidate)
            }
            throw ProxyTunnelException(ProxyFailure.OTHER, "The local proxy bridge was contacted by another socket")
        }
    }
}

private fun pump(
    from: InputStream,
    to: OutputStream,
    onEnd: () -> Unit,
) {
    val buffer = ByteArray(PUMP_BUFFER_BYTES)
    try {
        while (true) {
            val read = from.read(buffer)
            if (read < 0) break
            to.write(buffer, 0, read)
            to.flush()
        }
    } catch (_: IOException) {
        // A broken leg ends the whole connection; the HTTP client sees a closed socket.
    } finally {
        onEnd()
    }
}

private fun pumpThread(
    name: String,
    body: () -> Unit,
) {
    Thread(body, name).apply { isDaemon = true }.start()
}

internal fun closeQuietly(socket: Socket?) {
    try {
        socket?.close()
    } catch (_: IOException) {
        // Already closed or reset: nothing left to release.
    }
}

private const val PUMP_BUFFER_BYTES = 16 * 1024
private const val ACCEPT_TIMEOUT_MILLIS = 10_000
private const val MAX_FOREIGN_CONNECTIONS = 4
