package com.solthariel.pawchan.data.network.proxy

/** Checks the literal without any lookup. Zone identifiers are not accepted. */
internal fun validIpv6(host: String): Boolean {
    val shapeValid =
        host.length <= MAX_IPV6_LENGTH && ":::" !in host && host.indexOf("::") == host.lastIndexOf("::") &&
            host.all { it in '0'..'9' || it in 'a'..'f' || it == ':' || it == '.' }
    val groups = host.split("::", limit = 2).flatMap { if (it.isEmpty()) emptyList() else it.split(':') }
    return shapeValid && validIpv6Groups(groups, compressed = "::" in host)
}

/** Eight groups, or fewer around a single `::`; the last two may be written as an IPv4 tail. */
private fun validIpv6Groups(
    groups: List<String>,
    compressed: Boolean,
): Boolean {
    val tail = groups.lastOrNull()?.takeIf { '.' in it }
    val hexGroups = if (tail == null) groups else groups.dropLast(1)
    val count = hexGroups.size + if (tail == null) 0 else IPV4_TAIL_GROUPS
    val groupsValid = hexGroups.all(::validHexGroup)
    val tailValid = tail == null || validIpv4(tail)
    return groupsValid && tailValid && if (compressed) count < IPV6_GROUPS else count == IPV6_GROUPS
}

private fun validHexGroup(group: String): Boolean =
    group.length in 1..MAX_HEX_GROUP_LENGTH && group.all { it in '0'..'9' || it in 'a'..'f' }

private const val MAX_IPV6_LENGTH = 45
private const val IPV4_TAIL_GROUPS = 2
private const val IPV6_GROUPS = 8
private const val MAX_HEX_GROUP_LENGTH = 4
