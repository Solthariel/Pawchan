package com.solthariel.pawchan.data.network.proxy

import com.solthariel.pawchan.domain.proxy.ProxyService
import com.solthariel.pawchan.domain.settings.validMirrorHost
import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Outcome of one resource check. Time is the HTTP response time through the proxy, not an ICMP ping. */
sealed interface ProxyProbeResult {
    data class Response(
        val code: Int,
        val elapsedMillis: Long,
    ) : ProxyProbeResult

    /** Nothing was sent: no proxy is selected for the resource, or the resource cannot be checked. */
    data object Unavailable : ProxyProbeResult

    data class Failed(
        val reason: ProxyFailure,
    ) : ProxyProbeResult
}

/** Checks one resource via an isolated snapshot of the selected proxy; never falls back to direct. */
class ProxyResourceProbe(
    private val router: ScopedProxySelector,
    private val trust: ProxyTlsTrust = ProxyTlsTrust.system(),
) {
    suspend fun check(
        service: ProxyService,
        mirror: String,
    ): ProxyProbeResult {
        val prepared = prepareProbe(service, mirror) ?: return ProxyProbeResult.Unavailable
        val result = probeTarget(prepared.router, prepared.target, OkHttpClient.Builder(), trust)
        return if (router.route(service) == prepared.route) result else ProxyProbeResult.Unavailable
    }

    private fun prepareProbe(
        service: ProxyService,
        mirror: String,
    ): PreparedProbe? {
        val route = router.route(service) as? ProxyRoute.Proxied
        val target = probeUrl(service, mirror)
        if (route == null || target == null) return null
        val isolated = ScopedProxySelector(null)
        if (service == ProxyService.DVACH) isolated.registerMirror(mirror)
        isolated.update(service, route)
        return if (isolated.serviceForHost(target.host) == service) PreparedProbe(isolated, target, route) else null
    }
}

private data class PreparedProbe(
    val router: ScopedProxySelector,
    val target: HttpUrl,
    val route: ProxyRoute,
)

internal suspend fun probeTarget(
    router: ScopedProxySelector,
    target: HttpUrl,
    builder: OkHttpClient.Builder,
    trust: ProxyTlsTrust = ProxyTlsTrust.system(),
): ProxyProbeResult {
    val service = router.serviceForHost(target.host)
    val route = service?.let(router::route)
    if (!target.isHttps || route !is ProxyRoute.Proxied) return ProxyProbeResult.Unavailable
    val client =
        builder
            .viaProxyRouter(router, trust)
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    val start = System.nanoTime()
    return try {
        val request =
            Request
                .Builder()
                .url(target)
                .header("Cache-Control", "no-cache")
                .build()
        client.newCall(request).executeAsync().use { response ->
            ProxyProbeResult.Response(response.code, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start))
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: IOException) {
        ProxyProbeResult.Failed(failure.proxyFailure())
    } finally {
        client.dispatcher.cancelAll()
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }
}

private fun probeUrl(
    service: ProxyService,
    mirror: String,
): HttpUrl? =
    when (service) {
        ProxyService.DVACH -> if (validMirrorHost(mirror)) "https://$mirror/index.json".toHttpUrl() else null
        ProxyService.FOUR_CHAN -> "https://a.4cdn.org/boards.json".toHttpUrl()
        ProxyService.ARCHIVE -> "https://arhivach.vc/".toHttpUrl()
        ProxyService.VOCAROO -> "https://vocaroo.com/".toHttpUrl()
        ProxyService.GITHUB -> "https://api.github.com/".toHttpUrl()
    }

private const val PROBE_TIMEOUT_SECONDS = 10L
