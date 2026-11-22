package com.solthariel.pawchan.data.network.proxy

import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.Route
import java.io.IOException

/** Installed by [viaProxyRouter] as a network interceptor: it sees the connection that is really used. */
class ProxyRedirectGuard(
    private val router: ScopedProxySelector,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val connection = chain.connection()
        if (!proxyConnectionAllowed(router, request.url, connection?.route())) {
            // A pooled connection from before the route changed must not be reused by anyone.
            connection?.socket()?.let(::closeQuietly)
            val blocked = router.serviceForHost(request.url.host)?.let(router::route) == ProxyRoute.Unavailable
            throw if (blocked) {
                ProxyTunnelException(ProxyFailure.NOT_READY, "The proxy for this resource is not ready")
            } else {
                StaleProxyConnectionException()
            }
        }
        val response = chain.proceed(request)
        val location = response.header("Location")
        if (response.code !in REDIRECT_CODES || location == null) return response
        val destination = response.request.url.resolve(location)
        if (proxyRedirectAllowed(router, response.request.url, destination)) return response
        response.close()
        throw IOException("Redirect would escape the selected proxy service")
    }
}

/** The guard dropped a pooled connection that was opened before the route changed. */
internal class StaleProxyConnectionException : IOException("The connection was opened for a previous proxy route")

/**
 * Replaces a dropped stale connection by a fresh one on the current route, for GET and HEAD only.
 * The guard runs before the request is written, so nothing was sent on the stale connection; the
 * method restriction keeps that guarantee from being the only thing standing between a user action
 * (a post, a vote) and a duplicate. Other methods fail once with a clear error and are retried by the user.
 */
internal class StaleProxyConnectionRetry : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (stale: StaleProxyConnectionException) {
            if (request.method != "GET" && request.method != "HEAD") throw stale
            chain.proceed(request)
        }
    }
}

internal fun proxyRedirectAllowed(
    router: ScopedProxySelector,
    from: HttpUrl,
    to: HttpUrl?,
): Boolean {
    val service = router.serviceForHost(from.host) ?: return true
    return router.route(service) == ProxyRoute.Direct ||
        (to != null && to.isHttps && router.serviceForHost(to.host) == service)
}

private val REDIRECT_CODES =
    setOf(
        HTTP_MOVED_PERMANENTLY,
        HTTP_FOUND,
        HTTP_SEE_OTHER,
        HTTP_TEMPORARY_REDIRECT,
        HTTP_PERMANENT_REDIRECT,
    )
private const val HTTP_MOVED_PERMANENTLY = 301
private const val HTTP_FOUND = 302
private const val HTTP_SEE_OTHER = 303
private const val HTTP_TEMPORARY_REDIRECT = 307
private const val HTTP_PERMANENT_REDIRECT = 308

/** Protect against pooled connections created before a route changed. */
internal fun proxyConnectionAllowed(
    router: ScopedProxySelector,
    url: HttpUrl,
    route: Route?,
): Boolean {
    val service = router.serviceForHost(url.host) ?: return true
    val slot = router.slot(service)
    val placeholder = parsePlaceholder(route?.socketAddress?.address)
    return when (slot.route) {
        ProxyRoute.Direct -> {
            placeholder == null
        }

        ProxyRoute.Unavailable -> {
            false
        }

        is ProxyRoute.Proxied -> {
            url.isHttps && placeholder?.service == service && placeholder.epoch == slot.epoch &&
                placeholder.hostDigest == hostDigest(url.host)
        }
    }
}
