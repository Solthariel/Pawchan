package com.solthariel.pawchan.settings

/** Split a pasted authority only when its host/port boundary is unambiguous. Validation remains separate. */
internal fun pastedProxyAuthority(
    previous: String,
    input: String,
): Pair<String, String>? {
    if (insertedCharacters(previous, input) < 2) return null
    return authorityParts(input.trim())?.takeIf { (host, port) -> validPastedParts(host, port) }
}

private fun validPastedParts(
    host: String,
    port: String,
): Boolean =
    host.isNotEmpty() && port.isNotEmpty() && port.all { it in '0'..'9' } &&
        host.none { it.isWhitespace() || it in FORBIDDEN_HOST_CHARACTERS }

private fun authorityParts(text: String): Pair<String, String>? =
    when {
        text.startsWith('[') -> bracketedParts(text)
        text.count { it == ':' } == 1 -> text.substringBefore(':') to text.substringAfter(':')
        else -> null
    }

private fun bracketedParts(text: String): Pair<String, String>? {
    val close = text.indexOf(']')
    val bracketedIpv6 = close >= 0 && text.getOrNull(close + 1) == ':' && ':' in text.substring(1, close)
    return if (bracketedIpv6) text.substring(1, close) to text.substring(close + 2) else null
}

private fun insertedCharacters(
    previous: String,
    input: String,
): Int {
    val prefix = previous.commonPrefixWith(input).length
    val oldTail = previous.substring(prefix)
    val newTail = input.substring(prefix)
    return newTail.length - oldTail.commonSuffixWith(newTail).length
}

private val FORBIDDEN_HOST_CHARACTERS = setOf('@', '/', '?', '#', '\\', '[', ']')
