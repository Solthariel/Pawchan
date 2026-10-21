package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.posting.PostingProgressObserver
import com.solthariel.pawchan.domain.posting.PostingUploadProgress
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink

/** Request-owned: bounded buffers, no file copies, reset on HTTP replay. */
internal class PostingUploadTracker(
    private val sizes: List<Long>,
    private val observer: PostingProgressObserver,
    private val clock: () -> Long = System::nanoTime,
) {
    private val total = sizes.sum()
    private var sent = 0L
    private var lastReport = 0L

    fun file(
        index: Int,
        bytes: ByteArray,
        mediaType: MediaType? = null,
    ): RequestBody =
        object : RequestBody() {
            override fun contentType() = mediaType

            override fun contentLength() = bytes.size.toLong()

            override fun writeTo(sink: BufferedSink) {
                val before = sizes.take(index).sum()
                var offset = 0
                while (offset < bytes.size) {
                    val count = minOf(UPLOAD_CHUNK_BYTES, bytes.size - offset)
                    sink.write(bytes, offset, count)
                    offset += count
                    sent = before + offset
                    report()
                }
            }
        }

    fun request(body: RequestBody): RequestBody =
        object : RequestBody() {
            override fun contentType() = body.contentType()

            override fun contentLength() = body.contentLength()

            override fun writeTo(sink: BufferedSink) {
                sent = 0
                lastReport = 0
                report(force = true)
                body.writeTo(sink)
                sink.emit()
                // All form bytes have been handed to OkHttp, not a successful server response.
                report(force = true, awaiting = true)
            }
        }

    private fun report(
        force: Boolean = false,
        awaiting: Boolean = false,
    ) {
        val now = clock()
        if (force || now - lastReport >= REPORT_INTERVAL_NANOS) {
            lastReport = now
            observer.onProgress(PostingUploadProgress(sent, total, awaiting))
        }
    }
}

private const val REPORT_INTERVAL_NANOS = 100_000_000L

private const val UPLOAD_CHUNK_BYTES = 8 * 1024
