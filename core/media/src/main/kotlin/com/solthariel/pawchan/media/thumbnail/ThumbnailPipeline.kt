package com.solthariel.pawchan.media.thumbnail

import android.util.Log
import com.solthariel.pawchan.data.network.ServerCoolingDownException
import com.solthariel.pawchan.data.network.ServerRequestBackoff
import com.solthariel.pawchan.data.network.serverBackoff
import com.solthariel.pawchan.media.MEDIA_RATE_LIMITED_HTTP_STATUS
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.coroutines.executeAsync
import java.io.IOException
import java.time.Duration

internal class ThumbnailPipeline(
    baseClient: OkHttpClient,
    private val disk: ThumbnailDiskCache?,
    private val dispatcher: CoroutineDispatcher,
    density: Float,
) {
    private val client = baseClient.newBuilder().callTimeout(Duration.ofSeconds(CALL_TIMEOUT_SECONDS)).build()
    private val policy = client.serverBackoff() ?: ServerRequestBackoff()
    private val permits = Semaphore(REQUEST_LIMIT)
    private val codec = ThumbnailCodec(density)

    suspend fun load(task: ThumbnailTask): ThumbnailResult {
        val cached = runInterruptible(dispatcher) { disk?.load(task.key) }
        return if (cached != null) ThumbnailResult(cached) else loadNetwork(task)
    }

    private suspend fun loadNetwork(task: ThumbnailTask): ThumbnailResult {
        awaitReady(task)
        val request =
            Request
                .Builder()
                .url(task.url)
                .get()
                .build()
        var result: ThumbnailResult? = null
        repeat(MAX_ATTEMPTS) {
            if (result == null) result = attempt(task, request)
        }
        return result ?: ThumbnailResult()
    }

    private suspend fun attempt(
        task: ThumbnailTask,
        request: Request,
    ): ThumbnailResult? {
        val response = fetch(task, request) ?: return null
        return try {
            response.use { decodeResponse(task, it) }
        } finally {
            permits.release()
        }
    }

    private suspend fun decodeResponse(
        task: ThumbnailTask,
        response: Response,
    ): ThumbnailResult? =
        when {
            response.code == MEDIA_RATE_LIMITED_HTTP_STATUS -> {
                if (client.serverBackoff() == null) {
                    val host = response.request.url.host
                    policy.rateLimited(host, response.header("Retry-After"), response.header("Date"))
                }
                Log.w(
                    "PawchanThumbnail",
                    "HTTP 429: cooldown=${policy.remainingMillis(response.request.url.host)} ms",
                )
                null
            }

            response.code in MISSING_STATUSES -> {
                ThumbnailResult(failureMillis = MISSING_THUMBNAIL_MILLIS)
            }

            !response.isSuccessful -> {
                throw IOException("HTTP ${response.code} for ${task.url}")
            }

            else -> {
                val bitmap = runInterruptible(dispatcher) { codec.decode(response.body) }
                runInterruptible(dispatcher) {
                    disk?.store(task.key, bitmap)
                    ThumbnailResult(codec.hardware(bitmap))
                }
            }
        }

    private suspend fun fetch(
        task: ThumbnailTask,
        request: Request,
    ): Response? {
        while (true) {
            awaitReady(task)
            val started =
                try {
                    startRequest(task, request)
                } catch (_: ServerCoolingDownException) {
                    return null
                }
            if (started.response != null) return started.response
            delay(started.waitMillis.coerceAtLeast(READY_POLL_MILLIS))
        }
    }

    private suspend fun startRequest(
        task: ThumbnailTask,
        request: Request,
    ): ThumbnailRequestStart {
        var waitMillis = READY_POLL_MILLIS
        var response: Response? = null
        permits.acquire()
        try {
            if (task.ready) waitMillis = policy.previewStartDelayMillis(request.url.host)
            if (task.ready && waitMillis == 0L) response = client.newCall(request).executeAsync()
            return ThumbnailRequestStart(response, waitMillis)
        } finally {
            if (response == null) permits.release()
        }
    }

    private suspend fun awaitReady(task: ThumbnailTask) {
        checkNotNull(task.startGate).ready.first { it }
    }
}

private data class ThumbnailRequestStart(
    val response: Response?,
    val waitMillis: Long,
)

internal fun thumbnailFailure(
    url: String,
    exception: Exception,
): ThumbnailResult {
    Log.w("PawchanThumbnail", "Failed to load $url", exception)
    return ThumbnailResult()
}

private const val REQUEST_LIMIT = 3
private const val MAX_ATTEMPTS = 3
private const val READY_POLL_MILLIS = 50L
private const val CALL_TIMEOUT_SECONDS = 30L
private const val NOT_FOUND_STATUS = 404
private const val GONE_STATUS = 410
private val MISSING_STATUSES = setOf(NOT_FOUND_STATUS, GONE_STATUS)
