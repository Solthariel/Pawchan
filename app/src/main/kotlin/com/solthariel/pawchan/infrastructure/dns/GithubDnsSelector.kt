package com.solthariel.pawchan.infrastructure.dns

import com.solthariel.pawchan.domain.settings.ServiceDns
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.net.InetAddress
import java.net.UnknownHostException

/** GitHub update hosts only; unrelated hosts always use the original DNS. */
internal class GithubDnsSelector(
    bootstrapClient: OkHttpClient,
    private val systemDns: Dns = bootstrapClient.dns,
    private val createResolver: (ServiceDns) -> Dns? = { settings ->
        dohEndpoint(settings)?.let { createDohResolver(bootstrapClient, it) }
    },
) : Dns {
    @Volatile
    private var resolver: Pair<ServiceDns, Dns?> = ServiceDns() to null

    fun update(settings: ServiceDns) {
        if (resolver.first == settings) return
        resolver = settings to createResolver(settings)
    }

    override fun lookup(hostname: String): List<InetAddress> {
        val selected = resolver.second.takeIf { isGithubHost(hostname) }
        if (selected == null) return systemDns.lookup(hostname)
        return try {
            selected.lookup(hostname).ifEmpty { throw UnknownHostException("Empty DoH response") }
        } catch (_: UnknownHostException) {
            systemDns.lookup(hostname)
        }
    }
}

internal fun isGithubHost(host: String): Boolean {
    val name = host.lowercase().trimEnd('.')
    return name == "github.com" || name.endsWith(".github.com") ||
        name == "githubusercontent.com" || name.endsWith(".githubusercontent.com") ||
        name == "githubassets.com" || name.endsWith(".githubassets.com")
}
