package com.solthariel.pawchan.media

import kotlinx.coroutines.ensureActive
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

/** Caller owns streams, dispatcher and cancellation of blocking transport reads. */
suspend fun copyCancellableBytes(
    source: InputStream,
    destination: OutputStream,
    maxBytes: Long = Long.MAX_VALUE,
    limitMessage: String = "Download exceeds byte limit",
): Long {
    require(maxBytes >= 0)
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    try {
        while (true) {
            coroutineContext.ensureActive()
            val count = source.read(buffer)
            if (count < 0) return total
            coroutineContext.ensureActive()
            if (count > maxBytes - total) throw IOException(limitMessage)
            destination.write(buffer, 0, count)
            total += count
        }
    } catch (failure: IOException) {
        // Closing a blocking stream is how transport cancellation wakes the reader.
        coroutineContext.ensureActive()
        throw failure
    }
}
