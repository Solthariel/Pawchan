package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response

/** One gate owns all API requests for this client, including conditional refreshes. */
internal class FourChanTransport(
    private val calls: Call.Factory,
    private val clock: () -> Long = { System.nanoTime() / NANOS_PER_MILLISECOND },
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    private val gate = Mutex()
    private var lastStart: Long? = null
    private val threadStarts = mutableMapOf<String, Long>()
    private val cache = linkedMapOf<String, CachedFourChanResponse>()

    suspend fun get(
        segments: List<String>,
        isThread: Boolean = false,
    ): DomainOutcome<String> = fetch(segments, isThread) { DomainOutcome.Success(it) }

    suspend fun <T> fetch(
        segments: List<String>,
        isThread: Boolean = false,
        decode: (String) -> DomainOutcome<T>,
    ): DomainOutcome<T> =
        withContext(Dispatchers.Default) {
            gate.withLock {
                val url = fourChanApiUrl(segments)
                val key = url.toString()
                val now = clock()
                threadStarts.entries.removeAll { now - it.value >= THREAD_INTERVAL_MILLIS }
                val lastThread = threadStarts[key]
                val cached = cache[key]
                val recentThread = isThread && lastThread != null && now - lastThread < THREAD_INTERVAL_MILLIS
                if (recentThread && cached != null) {
                    return@withLock decode(cached.payload).also { if (it is DomainOutcome.Failure) cache.remove(key) }
                }
                awaitRequestSlot(key, isThread, now, lastThread)
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Accept", "application/json")
                        .header("User-Agent", FOUR_CHAN_USER_AGENT)
                        .apply { cached?.lastModified?.let { header("If-Modified-Since", it) } }
                        .get()
                        .build()
                cache.remove(key)
                var candidate: CachedFourChanResponse? = null
                when (
                    val result =
                        calls.executeHttpCall(request, HttpTransportFailure::toDomainOutcome) { response ->
                            responseToFourChanOutcome(response, cached) { candidate = it }
                        }
                ) {
                    is DomainOutcome.Failure -> {
                        result
                    }

                    is DomainOutcome.Success -> {
                        decode(result.value).also { parsed ->
                            if (parsed is DomainOutcome.Success) candidate?.let { remember(key, it) }
                        }
                    }
                }
            }
        }

    // Called with gate held: checking the interval and recording the start are one operation.
    private suspend fun awaitRequestSlot(
        key: String,
        isThread: Boolean,
        now: Long,
        lastThread: Long?,
    ) {
        val threadWait = if (isThread && lastThread != null) THREAD_INTERVAL_MILLIS - (now - lastThread) else 0L
        val globalWait = lastStart?.let { REQUEST_INTERVAL_MILLIS - (now - it) } ?: 0L
        val wait = maxOf(0L, threadWait, globalWait)
        if (wait > 0) sleep(wait)
        val started = clock()
        lastStart = started
        if (isThread) threadStarts[key] = started
    }

    private fun remember(
        key: String,
        value: CachedFourChanResponse,
    ) {
        cache.remove(key)
        cache[key] = value
        while (cache.size > CACHE_SIZE) cache.remove(cache.keys.first())
    }
}

private fun fourChanApiUrl(segments: List<String>): HttpUrl =
    HttpUrl
        .Builder()
        .scheme("https")
        .host("a.4cdn.org")
        .apply {
            segments.forEach { addPathSegment(it) }
        }.build()

private fun responseToFourChanOutcome(
    response: Response,
    cached: CachedFourChanResponse?,
    save: (CachedFourChanResponse) -> Unit,
): DomainOutcome<String> =
    when {
        response.code == NOT_MODIFIED && cached != null -> {
            save(cached)
            DomainOutcome.Success(cached.payload)
        }

        response.code == NOT_MODIFIED -> {
            DomainOutcome.Failure(DomainFailure.InvalidResponse)
        }

        response.code == NOT_FOUND || response.code == GONE -> {
            DomainOutcome.Failure(DomainFailure.NotFound)
        }

        !response.isSuccessful -> {
            DomainOutcome.Failure(DomainFailure.HttpStatus(response.code))
        }

        else -> {
            val source = response.body.source()
            if (source.request(MAX_JSON_BYTES + 1)) {
                DomainOutcome.Failure(DomainFailure.InvalidResponse)
            } else {
                val payload = source.readUtf8()
                save(CachedFourChanResponse(payload, response.header("Last-Modified")))
                DomainOutcome.Success(payload)
            }
        }
    }

private data class CachedFourChanResponse(
    val payload: String,
    val lastModified: String?,
)

private const val FOUR_CHAN_USER_AGENT = "Pawchan/0.1 (unofficial 4chan client)"
private const val REQUEST_INTERVAL_MILLIS = 1_000L
private const val THREAD_INTERVAL_MILLIS = 10_000L
private const val NANOS_PER_MILLISECOND = 1_000_000L
private const val MAX_JSON_BYTES = 12L * 1024L * 1024L
private const val CACHE_SIZE = 8

private const val NOT_MODIFIED = 304
private const val NOT_FOUND = 404
private const val GONE = 410
