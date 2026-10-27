package com.solthariel.pawchan.data.network

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLException

internal fun MirrorRoutingInterceptor.readWithFallback(chain: Interceptor.Chain): Response {
    val request = route(chain.request())
    return if (!chain.request().isMirrorRead() || recovery == null) {
        chain.proceed(request)
    } else {
        val attempt = mirrorReadAttempt(chain, request)
        val switch = attempt.needsRecovery && awaitMirror(chain, request.url.host) != null
        if (switch) {
            attempt.response?.close()
            val retry =
                chain
                    .request()
                    .newBuilder()
                    .removeHeader("Cookie")
                    .removeHeader("Authorization")
                    .build()
            chain.proceed(route(retry))
        } else {
            attempt.response ?: throw requireNotNull(attempt.failure)
        }
    }
}

private data class MirrorReadAttempt(
    val response: Response? = null,
    val failure: IOException? = null,
) {
    val needsRecovery: Boolean get() = failure != null || response?.code in TEMPORARY_MIRROR_ERRORS
}

private fun mirrorReadAttempt(
    chain: Interceptor.Chain,
    request: Request,
): MirrorReadAttempt =
    try {
        MirrorReadAttempt(response = chain.proceed(request))
    } catch (security: SSLException) {
        throw security
    } catch (cooldown: ServerCoolingDownException) {
        throw cooldown
    } catch (failure: IOException) {
        if (chain.call().isCanceled()) throw failure
        MirrorReadAttempt(failure = failure)
    }

internal fun Request.isMirrorRead(): Boolean =
    method == "GET" && url.isHttps && url.encodedPath.endsWith(".json") && header("Range") == null &&
        url.host in setOf("2ch.su", "2ch.org", "2ch.life", "2ch.hk", "2ch.pm")

private fun MirrorRoutingInterceptor.awaitMirror(
    chain: Interceptor.Chain,
    failedHost: String,
): String? {
    val future = recovery?.recover(failedHost) ?: return null
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(MIRROR_WAIT_SECONDS)
    var complete = false
    var selected: String? = null
    while (!complete && System.nanoTime() < deadline) {
        if (chain.call().isCanceled()) throw IOException("Canceled")
        try {
            selected = future.get(CANCELLATION_POLL_MILLIS, TimeUnit.MILLISECONDS)
            complete = true
        } catch (_: TimeoutException) {
            // Poll cancellation while sharing the same check, not a new request.
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw InterruptedIOException("Mirror wait interrupted").apply { initCause(interrupted) }
        } catch (_: ExecutionException) {
            complete = true
        }
    }
    return selected
}

private val TEMPORARY_MIRROR_ERRORS = setOf(BAD_GATEWAY_STATUS, UNAVAILABLE_STATUS, GATEWAY_TIMEOUT_STATUS)
private const val MIRROR_WAIT_SECONDS = 5L
private const val CANCELLATION_POLL_MILLIS = 100L

private const val BAD_GATEWAY_STATUS = 502
private const val UNAVAILABLE_STATUS = 503
private const val GATEWAY_TIMEOUT_STATUS = 504
