package com.solthariel.pawchan.infrastructure.dns

import com.solthariel.pawchan.domain.settings.ServiceDns
import okhttp3.CookieJar
import okhttp3.Dns
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration

/** Only Arhivach uses this resolver; all other hosts keep the client's original DNS. */
internal class ArhivachDnsSelector(
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
        val selected = resolver.second.takeIf { isArhivachHost(hostname) }
        if (selected == null) return systemDns.lookup(hostname)
        return try {
            selected.lookup(hostname).ifEmpty { throw UnknownHostException("Empty DoH response") }
        } catch (_: UnknownHostException) {
            systemDns.lookup(hostname)
        }
    }
}

internal fun isArhivachHost(host: String): Boolean {
    val name = host.lowercase().trimEnd('.')
    return name == "arhivach.vc" || name.endsWith(".arhivach.vc")
}

/** Background workers borrow the same client resources and close them in their existing finally. */
internal fun OkHttpClient.withArhivachDns(settings: ServiceDns): OkHttpClient =
    newBuilder().dns(ArhivachDnsSelector(this).apply { update(settings) }).build()

internal fun createDohResolver(
    bootstrap: OkHttpClient,
    endpoint: DohEndpoint,
): Dns {
    val client =
        bootstrap
            .newBuilder()
            .dns(Dns.SYSTEM)
            .cookieJar(CookieJar.NO_COOKIES)
            .eventListener(EventListener.NONE)
            .callTimeout(DOH_TIMEOUT)
            .connectTimeout(DOH_TIMEOUT)
            .build()
    return DnsOverHttps
        .Builder()
        .client(client)
        .url(endpoint.url)
        .apply { if (endpoint.bootstrap.isNotEmpty()) bootstrapDnsHosts(endpoint.bootstrap) }
        .resolvePrivateAddresses(false)
        .build()
}

private const val DOH_TIMEOUT_SECONDS = 3L
private val DOH_TIMEOUT: Duration = Duration.ofSeconds(DOH_TIMEOUT_SECONDS)
