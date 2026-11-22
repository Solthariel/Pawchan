package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import java.io.InputStream
import java.io.OutputStream

/** RFC 1928 CONNECT with the host name sent to the proxy, so the resource is never resolved locally. */
internal fun socks5Connect(
    input: InputStream,
    output: OutputStream,
    host: String,
    port: Int,
    credentials: ProxyCredentials?,
) {
    val hostBytes = host.toByteArray(Charsets.US_ASCII)
    ensure(hostBytes.size in 1..MAX_SOCKS_HOST_BYTES, ProxyFailure.OTHER) { "Host name is too long for SOCKS5" }
    socks5Greet(input, output, credentials)
    val request =
        byteArrayOf(SOCKS_VERSION, SOCKS_CONNECT, 0, SOCKS_DOMAIN, hostBytes.size.toByte()) + hostBytes +
            byteArrayOf((port ushr BYTE_BITS).toByte(), port.toByte())
    output.write(request)
    output.flush()
    val head = readFully(input, SOCKS_REPLY_HEAD_BYTES)
    ensure(head[0] == SOCKS_VERSION, ProxyFailure.BAD_PROXY_RESPONSE) { "Invalid SOCKS5 reply" }
    val reply = head[1].toInt() and BYTE_MASK
    ensure(reply == SOCKS_SUCCEEDED.toInt(), ProxyFailure.TARGET_UNREACHABLE) { socksRefusal(reply) }
    readFully(input, socksBoundAddressBytes(input, head[SOCKS_ADDRESS_TYPE_INDEX]) + PORT_BYTES)
}

/** Offers only the login methods the profile can satisfy and runs the one the proxy picks. */
private fun socks5Greet(
    input: InputStream,
    output: OutputStream,
    credentials: ProxyCredentials?,
) {
    val methods = if (credentials == null) byteArrayOf(SOCKS_NO_AUTH) else byteArrayOf(SOCKS_NO_AUTH, SOCKS_PASSWORD)
    output.write(byteArrayOf(SOCKS_VERSION, methods.size.toByte()) + methods)
    output.flush()
    val choice = readFully(input, 2)
    ensure(choice[0] == SOCKS_VERSION, ProxyFailure.BAD_PROXY_RESPONSE) { "The server is not a SOCKS5 proxy" }
    when (choice[1]) {
        SOCKS_NO_AUTH -> Unit
        SOCKS_PASSWORD -> socks5Authenticate(input, output, credentials)
        else -> throw authFailure(credentials, "SOCKS5 proxy accepts no available login method")
    }
}

/** RFC 1929 username/password sub-negotiation. */
private fun socks5Authenticate(
    input: InputStream,
    output: OutputStream,
    credentials: ProxyCredentials?,
) {
    if (credentials == null) throw tunnelError(ProxyFailure.AUTH_REQUIRED, "The proxy requires a login and password")
    val user = credentials.username.toByteArray(Charsets.UTF_8)
    val password = credentials.password.toByteArray(Charsets.UTF_8)
    output.write(
        byteArrayOf(SOCKS_AUTH_VERSION, user.size.toByte()) + user + byteArrayOf(password.size.toByte()) + password,
    )
    output.flush()
    // Only the status byte matters; some servers echo the SOCKS version instead of the sub-negotiation one.
    val reply = readFully(input, 2)
    ensure(reply[1] == SOCKS_SUCCEEDED, ProxyFailure.AUTH_REJECTED) { "The proxy rejected the login or password" }
}

private fun socksBoundAddressBytes(
    input: InputStream,
    type: Byte,
): Int =
    when (type) {
        SOCKS_IPV4 -> IPV4_BYTES
        SOCKS_IPV6 -> IPV6_BYTES
        SOCKS_DOMAIN -> readFully(input, 1)[0].toInt() and BYTE_MASK
        else -> throw tunnelError(ProxyFailure.BAD_PROXY_RESPONSE, "Invalid SOCKS5 address type")
    }

private fun authFailure(
    credentials: ProxyCredentials?,
    detail: String,
): ProxyTunnelException =
    if (credentials == null) {
        tunnelError(ProxyFailure.AUTH_REQUIRED, "The proxy requires a login and password ($detail)")
    } else {
        tunnelError(ProxyFailure.AUTH_REJECTED, "The proxy refused the login method ($detail)")
    }

private fun socksRefusal(code: Int): String =
    when (code) {
        SOCKS_NOT_ALLOWED -> "The proxy rule set does not allow this connection"
        SOCKS_NETWORK_UNREACHABLE, SOCKS_HOST_UNREACHABLE -> "The proxy could not reach the resource"
        SOCKS_CONNECTION_REFUSED -> "The resource refused the connection from the proxy"
        else -> "The proxy could not open the connection (SOCKS5 code $code)"
    }

private const val SOCKS_VERSION: Byte = 5
private const val SOCKS_AUTH_VERSION: Byte = 1
private const val SOCKS_NO_AUTH: Byte = 0
private const val SOCKS_PASSWORD: Byte = 2
private const val SOCKS_CONNECT: Byte = 1
private const val SOCKS_IPV4: Byte = 1
private const val SOCKS_DOMAIN: Byte = 3
private const val SOCKS_IPV6: Byte = 4
private const val SOCKS_SUCCEEDED: Byte = 0
private const val SOCKS_NOT_ALLOWED = 2
private const val SOCKS_NETWORK_UNREACHABLE = 3
private const val SOCKS_HOST_UNREACHABLE = 4
private const val SOCKS_CONNECTION_REFUSED = 5
private const val SOCKS_REPLY_HEAD_BYTES = 4
private const val SOCKS_ADDRESS_TYPE_INDEX = 3
private const val MAX_SOCKS_HOST_BYTES = 255
private const val IPV4_BYTES = 4
private const val IPV6_BYTES = 16
private const val PORT_BYTES = 2
private const val BYTE_BITS = 8
private const val BYTE_MASK = 0xFF
