package com.solthariel.pawchan.infrastructure.vocaroo

import android.util.Log
import com.solthariel.pawchan.domain.settings.VocarooDns
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.UnknownHostException
import java.time.Duration

/**
 * DNS клиента Vocaroo: для хостов Vocaroo — выбранный DoH, при его ошибке или таймауте (~3 с) —
 * системный DNS. TLS проверяется как обычно: DoH меняет только то, где спрашивают адрес.
 */
internal class VocarooDnsSelector(
    private val bootstrapClient: OkHttpClient,
) : Dns {
    @Volatile
    private var resolver: Pair<VocarooDns, Dns?> = VocarooDns() to null

    /** Вызывается при изменении настройки; DoH-клиент пересоздаётся только для нового адреса. */
    fun update(dns: VocarooDns) {
        if (resolver.first == dns) return
        resolver = dns to vocarooDohEndpoint(dns)?.let(::dohFor)
    }

    val usesSystemDns: Boolean
        get() = resolver.second == null

    override fun lookup(hostname: String): List<InetAddress> {
        val doh = resolver.second.takeIf { isVocarooHost(hostname) } ?: return Dns.SYSTEM.lookup(hostname)
        val started = System.nanoTime()
        return try {
            doh.lookup(hostname).also { found ->
                val addresses = found.joinToString { it.hostAddress.orEmpty() }
                Log.i(TAG, "doh host=$hostname ${elapsedMillis(started)}ms addresses=$addresses")
            }
        } catch (failure: UnknownHostException) {
            Log.w(TAG, "doh host=$hostname failed after ${elapsedMillis(started)}ms (${failure.message}), system DNS")
            Dns.SYSTEM.lookup(hostname)
        }
    }

    private fun dohFor(endpoint: VocarooDohEndpoint): Dns {
        val client =
            bootstrapClient
                .newBuilder()
                .callTimeout(DOH_TIMEOUT)
                .connectTimeout(DOH_TIMEOUT)
                .eventListener(okhttp3.EventListener.NONE)
                .build()
        return DnsOverHttps
            .Builder()
            .client(client)
            .url(endpoint.url)
            .apply { if (endpoint.bootstrap.isNotEmpty()) bootstrapDnsHosts(endpoint.bootstrap) }
            .resolvePrivateAddresses(false)
            .build()
    }
}

internal fun elapsedMillis(startedNanos: Long): Long = (System.nanoTime() - startedNanos) / NANOS_IN_MILLI

internal const val TAG = "Vocaroo"
private const val NANOS_IN_MILLI = 1_000_000L
private const val DOH_TIMEOUT_SECONDS = 3L
private val DOH_TIMEOUT: Duration = Duration.ofSeconds(DOH_TIMEOUT_SECONDS)
