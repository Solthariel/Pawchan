package com.solthariel.pawchan.ffmpeg

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * One bounded, cancellable tail/range GET. A newer request supersedes the queued or running one.
 * A failed optional range never discards the sequential download; it only disables ranges.
 */
internal class RangeFetcher(
    private val client: OkHttpClient,
    private val url: String,
    private val timing: TimingProbe,
    private val closed: () -> Boolean,
    private val written: (Long, Long) -> Unit,
    private val unavailable: () -> Unit,
    private val onTerminated: () -> Unit,
) {
    private val worker =
        object : ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(1),
            ThreadPoolExecutor.DiscardOldestPolicy(),
        ) {
            override fun terminated() {
                try {
                    onTerminated()
                } finally {
                    super.terminated()
                }
            }
        }
    private val generation = AtomicLong()

    @Volatile private var call: Call? = null

    fun request(job: RangeJob) {
        val token = generation.get()
        worker.execute { fetch(job, token) }
    }

    fun cancel() {
        generation.incrementAndGet()
        call?.cancel()
    }

    fun shutdown() {
        cancel()
        worker.shutdownNow()
    }

    private fun current(token: Long): Boolean = !closed() && token == generation.get()

    private fun fetch(
        job: RangeJob,
        token: Long,
    ) {
        if (!current(token)) return
        val next =
            client.newCall(
                mediaRequest(url)
                    .header("Range", "bytes=${job.start}-")
                    .header("If-Range", job.tag.ifRange.orEmpty())
                    .build(),
            )
        call = next
        try {
            if (current(token)) next.execute().use { receive(it, job, token) } else next.cancel()
        } catch (_: IOException) {
            timing.mark(TimingProbe.Event.RANGE_ERROR, if (closed()) 1 else 0)
            if (current(token)) unavailable()
        } finally {
            if (call === next) call = null
        }
    }

    @Throws(IOException::class)
    private fun receive(
        response: Response,
        job: RangeJob,
        token: Long,
    ) {
        timing.mark(TimingProbe.Event.RANGE_HEADERS, response.code.toLong())
        if (rangeRefused(response)) {
            timing.mark(TimingProbe.Event.RANGE_UNAVAILABLE, response.code.toLong())
            unavailable()
        } else {
            val end = checkedRangeEnd(response, job.start, job.total, job.tag)
            copyBody(response.body.byteStream(), job.file, job.start, end, { current(token) }) { _, position ->
                written(job.start, position)
            }
        }
    }
}

/** A range of [file] starting at [start] for the entity identified by [tag] and [total] length. */
internal class RangeJob(
    val start: Long,
    val file: File,
    val total: Long,
    val tag: EntityTag,
)
