package com.solthariel.pawchan.data.network.proxy

/** Host and port of a proxy server. An IPv6 literal is stored without brackets. */
data class ProxyEndpoint(
    val host: String,
    val port: Int,
) {
    /** `host:port`, with an IPv6 literal in brackets. This is the form stored in a saved proxy. */
    val authority: String get() = if (':' in host) "[$host]:$port" else "$host:$port"
}
