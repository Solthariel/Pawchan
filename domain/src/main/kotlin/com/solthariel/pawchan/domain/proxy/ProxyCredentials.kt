package com.solthariel.pawchan.domain.proxy

/**
 * Login and password for a proxy server. The value never appears in [toString], so a log line,
 * an exception message or a crash report cannot reveal it by accident.
 */
data class ProxyCredentials(
    val username: String,
    val password: String,
) {
    override fun toString(): String = "ProxyCredentials(username=***, password=***)"
}

/**
 * SOCKS5 (RFC 1929) allows 1..255 bytes per field. HTTP Basic joins both with a colon, so the
 * login cannot contain one. Control characters would break the header or the handshake.
 */
fun validProxyCredentials(
    credentials: ProxyCredentials,
    mode: ProxyMode,
): Boolean {
    val (username, password) = credentials
    val sizesValid = listOf(username, password).all { it.toByteArray(Charsets.UTF_8).size in 1..MAX_CREDENTIAL_BYTES }
    val charsValid = username.none(Char::isISOControl) && password.none(Char::isISOControl)
    val colonValid = mode == ProxyMode.SOCKS5 || ':' !in username
    return mode.isProxy && sizesValid && charsValid && colonValid
}

private const val MAX_CREDENTIAL_BYTES = 255
