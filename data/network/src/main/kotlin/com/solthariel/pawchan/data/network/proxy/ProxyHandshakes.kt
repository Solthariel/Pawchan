package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyCredentials
import java.io.InputStream
import java.io.OutputStream
import java.util.Base64

/** HTTP CONNECT with Basic authentication when credentials are set. */
internal fun httpConnect(
    input: InputStream,
    output: OutputStream,
    host: String,
    port: Int,
    credentials: ProxyCredentials?,
) {
    output.write(connectRequest(host, port, credentials).toByteArray(Charsets.ISO_8859_1))
    output.flush()
    val status = parseStatusLine(readLine(input))
    skipHeaders(input)
    when (status) {
        in HTTP_SUCCESS_MIN..HTTP_SUCCESS_MAX -> Unit
        HTTP_PROXY_AUTH_REQUIRED -> throw httpAuthFailure(credentials)
        else -> throw tunnelError(ProxyFailure.TARGET_UNREACHABLE, "The proxy refused the connection (HTTP $status)")
    }
}

private fun connectRequest(
    host: String,
    port: Int,
    credentials: ProxyCredentials?,
): String {
    val authority = if (':' in host) "[$host]:$port" else "$host:$port"
    return buildString {
        append("CONNECT ").append(authority).append(" HTTP/1.1\r\n")
        append("Host: ").append(authority).append("\r\n")
        append("Proxy-Connection: keep-alive\r\n")
        if (credentials != null) append("Proxy-Authorization: Basic ").append(basicToken(credentials)).append("\r\n")
        append("\r\n")
    }
}

private fun httpAuthFailure(credentials: ProxyCredentials?): ProxyTunnelException =
    if (credentials == null) {
        tunnelError(ProxyFailure.AUTH_REQUIRED, "The proxy requires a login and password")
    } else {
        tunnelError(ProxyFailure.AUTH_REJECTED, "The proxy rejected the login or password")
    }

private fun basicToken(credentials: ProxyCredentials): String =
    Base64.getEncoder().encodeToString("${credentials.username}:${credentials.password}".toByteArray(Charsets.UTF_8))

private fun parseStatusLine(line: String): Int {
    val parts = line.split(' ', limit = 3)
    val code = parts.getOrNull(1)?.takeIf { it.length == STATUS_DIGITS && it.all(Char::isDigit) }?.toInt()
    ensure(parts[0].startsWith("HTTP/1.") && code != null, ProxyFailure.BAD_PROXY_RESPONSE) {
        "The server is not an HTTP proxy"
    }
    return requireNotNull(code)
}

private fun skipHeaders(input: InputStream) {
    var headerCount = 0
    while (readLine(input).isNotEmpty()) {
        headerCount += 1
        ensure(headerCount <= MAX_HEADERS, ProxyFailure.BAD_PROXY_RESPONSE) { "Too many proxy response headers" }
    }
}

private const val STATUS_DIGITS = 3
private const val MAX_HEADERS = 100
private const val HTTP_PROXY_AUTH_REQUIRED = 407
private const val HTTP_SUCCESS_MIN = 200
private const val HTTP_SUCCESS_MAX = 299
