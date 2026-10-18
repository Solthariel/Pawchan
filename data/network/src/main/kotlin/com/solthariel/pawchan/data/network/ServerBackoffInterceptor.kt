package com.solthariel.pawchan.data.network

import kotlinx.coroutines.delay
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

internal class ServerBackoffInterceptor(
    val policy: ServerRequestBackoff = ServerRequestBackoff(),
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host
        val remaining = policy.remainingMillis(host)
        if (remaining > 0L) throw ServerCoolingDownException(remaining)
        return chain.proceed(request).also { response ->
            if (response.code == RATE_LIMITED_STATUS) {
                policy.rateLimited(host, response.header("Retry-After"), response.header("Date"))
            }
        }
    }
}

class ServerCoolingDownException(
    val retryAfterMillis: Long,
) : IOException("Server rate-limit cooldown")

fun Call.Factory.serverBackoff(): ServerRequestBackoff? =
    (this as? OkHttpClient)
        ?.interceptors
        ?.filterIsInstance<ServerBackoffInterceptor>()
        ?.firstOrNull()
        ?.policy

/** Short, cancellable wait for API calls. A long server pause is returned as HTTP 429, not a hung UI. */
internal suspend fun Call.Factory.awaitServerAvailability(host: String) {
    val policy = serverBackoff() ?: return
    var budget = API_WAIT_BUDGET_MILLIS
    while (true) {
        val remaining = policy.remainingMillis(host)
        if (remaining == 0L) return
        if (remaining > budget) throw ServerCoolingDownException(remaining)
        delay(remaining)
        budget -= remaining
    }
}

internal const val RATE_LIMITED_STATUS = 429
private const val API_WAIT_BUDGET_MILLIS = 5_000L
