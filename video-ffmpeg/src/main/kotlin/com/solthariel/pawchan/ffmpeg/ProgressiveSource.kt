package com.solthariel.pawchan.ffmpeg

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Two bounded workers: one sequential GET and one cancellable tail/range GET.
 * Native code sees only published, actually written ranges of a session-owned file.
 * Cookies, redirects and HTTPS policy remain with the application's OkHttp client.
 */
class ProgressiveSource(
    private val client: OkHttpClient,
    private val directory: File,
    private val url: String,
    private val listener: Listener,
    private val timing: TimingProbe,
) : ProgressiveReadCallback,
    AutoCloseable {
    constructor(client: OkHttpClient, directory: File, url: String, listener: Listener) :
        this(client, directory, url, listener, TimingProbe.disabled())

    interface Listener {
        fun onFile(file: File)

        fun onError(error: IOException)
    }

    // A stopped writer signals termination; removal never waits on the mpv worker.
    private val sequential =
        object : ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(1)) {
            override fun terminated() {
                try {
                    sequentialStopped.set(true)
                    maybeDeleteFile()
                } finally {
                    super.terminated()
                }
            }
        }
    private val ranges =
        RangeFetcher(
            client,
            url,
            timing,
            closed = { closed },
            written = { start, end -> sink?.takeUnless { closed }?.publishTail(start, end) },
            unavailable = {
                rangesAllowed = false
                publishProgress()
            },
            onTerminated = {
                rangesStopped.set(true)
                maybeDeleteFile()
            },
        )

    @Volatile private var primaryCall: Call? = null

    @Volatile private var sink: ProgressiveInputSink? = null

    @Volatile var file: File? = null
        private set

    @Volatile var progress = 0L
        private set

    @Volatile private var total = -1L

    @Volatile private var tag = EntityTag(null, null)

    @Volatile private var closed = false

    private val sequentialStopped = AtomicBoolean(false)
    private val rangesStopped = AtomicBoolean(false)
    private val readerReleased = AtomicBoolean(false)
    private val deletionStarted = AtomicBoolean(false)

    @Volatile var isComplete = false
        private set

    @Volatile var failed = false
        private set

    @Volatile private var rangesAllowed = true

    fun start() {
        timing.mark(TimingProbe.Event.GET_START, 0)
        sequential.execute(::download)
    }

    fun bind(sink: ProgressiveInputSink?) {
        this.sink = sink
    }

    override fun onReadReady() {
        publishProgress()
    }

    private fun download() {
        try {
            val next = File.createTempFile("neko-ffmpeg-", ".media", directory)
            file = next
            val call = client.newCall(mediaRequest(url).get().build())
            primaryCall = call
            if (closed) call.cancel() else call.execute().use { receive(it, next) }
        } catch (error: IOException) {
            timing.mark(TimingProbe.Event.GET_ERROR, if (closed) 1 else 0)
            if (!closed) {
                failed = true
                listener.onError(error)
            }
        } finally {
            primaryCall = null
        }
    }

    @Throws(IOException::class)
    private fun receive(
        response: Response,
        target: File,
    ) {
        timing.mark(TimingProbe.Event.GET_HEADERS, response.code.toLong())
        val body = checkedPrimaryBody(response)
        total = body.contentLength()
        timing.mark(TimingProbe.Event.GET_LENGTH, total)
        if (total > MAX_MEDIA_BYTES) throw IOException("Video exceeds 1 GiB safety limit")
        tag = EntityTag.of(response)
        if (!closed) listener.onFile(target)
        copyBody(body.byteStream(), target, 0, total, { !closed }) { previous, end ->
            if (previous == 0L) timing.mark(TimingProbe.Event.GET_FIRST_BYTES, end)
            progress = end
            publishProgress()
        }
        if (progress == 0L || (total >= 0 && progress != total)) throw IOException("Incomplete video GET")
        total = progress
        isComplete = true
        timing.mark(TimingProbe.Event.GET_COMPLETE, progress)
        ranges.cancel()
        publishProgress()
    }

    private fun publishProgress() {
        val active = sink
        if (active != null && !closed) active.publish(progress, total)
    }

    override fun requestPartFromPosition(position: Long) {
        ranges.cancel()
        val target = file
        val unavailable = closed || isComplete || !rangesAllowed || target == null
        val outside = position <= progress || total < 0 || position >= total
        when {
            unavailable || outside -> {
                publishProgress()
            }

            tag.ifRange == null -> {
                timing.mark(TimingProbe.Event.RANGE_NO_VALIDATOR, position)
                rangesAllowed = false
                publishProgress()
            }

            else -> {
                timing.mark(TimingProbe.Event.RANGE_REQUEST, position)
                ranges.request(RangeJob(position, checkNotNull(target), total, tag))
            }
        }
    }

    override fun close() {
        timing.mark(TimingProbe.Event.SOURCE_CLOSE, progress)
        closed = true
        sink = null
        primaryCall?.cancel()
        ranges.shutdown()
        sequential.shutdownNow()
    }

    /** Called after mpv has destroyed the native reader. Does not wait for cancelled HTTP I/O. */
    fun deleteFile() {
        readerReleased.set(true)
        maybeDeleteFile()
    }

    /** Last writer to terminate removes the file, but only after the native reader is gone. */
    private fun maybeDeleteFile() {
        val readyForDeletion = readerReleased.get() && sequentialStopped.get() && rangesStopped.get()
        if (!readyForDeletion || !deletionStarted.compareAndSet(false, true)) return
        val target = file ?: return
        if (!target.delete() && target.exists()) target.deleteOnExit()
    }
}
