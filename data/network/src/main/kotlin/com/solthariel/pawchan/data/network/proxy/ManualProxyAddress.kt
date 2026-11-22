package com.solthariel.pawchan.data.network.proxy

import java.util.Locale

/**
 * Explicit user input names the proxy server. Credentials are separate fields, so `user@host`
 * is rejected: a password must never travel inside an address that is stored or displayed.
 */
fun parseManualSocks5Address(input: String): ProxyEndpoint? = parseProxyAddress(input, "socks5://")

fun parseManualHttpAddress(input: String): ProxyEndpoint? = parseProxyAddress(input, "http://")

fun parseManualHttpsAddress(input: String): ProxyEndpoint? = parseProxyAddress(input, "https://")

/** Parses `host:port`, `[ipv6]:port`, or the same with the expected scheme in front. */
fun parseProxyAddress(
    input: String,
    scheme: String = "",
): ProxyEndpoint? {
    if (input.length > MAX_INPUT_LENGTH) return null
    val trimmed = input.trim()
    val address = if (trimmed.startsWith(scheme, ignoreCase = true)) trimmed.substring(scheme.length) else trimmed
    val malformed = "://" in address || address.any { it.isWhitespace() || it in FORBIDDEN_ADDRESS_CHARS }
    return if (malformed) null else splitAuthority(address)?.let { (host, port) -> endpointOf(host, port) }
}

/**
 * The editor keeps the host and the port in separate fields. An IPv6 literal may be typed with
 * or without brackets; the result is the same canonical endpoint as [parseProxyAddress] gives.
 */
fun proxyEndpointOf(
    host: String,
    port: String,
): ProxyEndpoint? {
    val bare = host.trim().removeSurrounding("[", "]")
    val authority = if (':' in bare) "[$bare]:${port.trim()}" else "$bare:${port.trim()}"
    return parseProxyAddress(authority)
}

private fun splitAuthority(address: String): Pair<String, String>? =
    if (address.startsWith('[')) {
        val end = address.indexOf("]:")
        if (end < 0) null else address.substring(1, end) to address.substring(end + 2)
    } else {
        address.split(':').takeIf { it.size == 2 }?.let { it[0] to it[1] }
    }

private fun endpointOf(
    rawHost: String,
    rawPort: String,
): ProxyEndpoint? {
    val host = rawHost.lowercase(Locale.ROOT)
    val port = rawPort.takeIf { it.length in 1..MAX_PORT_DIGITS && it.all(Char::isDigit) }?.toInt()
    return if (port != null && port in 1..MAX_PORT && validHost(host)) ProxyEndpoint(host, port) else null
}

private fun validHost(host: String): Boolean =
    host.isNotEmpty() && host.length <= MAX_HOST_LENGTH &&
        when {
            ':' in host -> validIpv6(host)
            host.all { it.isDigit() || it == '.' } -> validIpv4(host)
            else -> validHostname(host)
        }

internal fun validIpv4(host: String): Boolean {
    val octets = host.split('.')
    return octets.size == IPV4_OCTET_COUNT && octets.all(::validOctet)
}

private fun validOctet(octet: String): Boolean =
    octet.isNotEmpty() && octet.length <= MAX_OCTET_LENGTH &&
        (octet.length == 1 || !octet.startsWith('0')) && octet.toIntOrNull()?.let { it in 0..MAX_OCTET } == true

private fun validHostname(host: String): Boolean =
    host.split('.').all { label ->
        label.isNotEmpty() && label.length <= MAX_LABEL_LENGTH && label.first() != '-' && label.last() != '-' &&
            label.all { it in 'a'..'z' || it in '0'..'9' || it == '-' }
    }

private val FORBIDDEN_ADDRESS_CHARS = setOf('@', '/', '?', '#', '\\')
private const val MAX_INPUT_LENGTH = 255
private const val MAX_HOST_LENGTH = 253
private const val MAX_LABEL_LENGTH = 63
private const val MAX_OCTET_LENGTH = 3
private const val IPV4_OCTET_COUNT = 4
private const val MAX_OCTET = 255
private const val MAX_PORT = 65535
private const val MAX_PORT_DIGITS = 5
