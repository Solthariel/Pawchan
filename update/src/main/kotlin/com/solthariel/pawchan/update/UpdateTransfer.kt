package com.solthariel.pawchan.update

import java.io.InputStream
import java.io.OutputStream

/** Streaming limit is the declared APK length, not just the absolute package ceiling. */
internal fun copyUpdateStream(
    input: InputStream,
    output: OutputStream,
    expectedLength: Long,
    onProgress: (Int) -> Unit,
    checkpoint: () -> Unit,
): Long {
    require(expectedLength in 1..MAX_APK_BYTES) { "Invalid update length" }
    val buffer = ByteArray(UPDATE_BUFFER_SIZE)
    var copied = 0L
    while (true) {
        checkpoint()
        val count = input.read(buffer)
        checkpoint()
        if (count < 0) break
        check(count.toLong() <= expectedLength - copied) { "Update exceeds declared length" }
        output.write(buffer, 0, count)
        copied += count
        onProgress(((copied * UPDATE_PERCENT) / expectedLength).toInt())
    }
    check(copied == expectedLength) { "Downloaded update length mismatch" }
    return copied
}

internal const val MAX_APK_BYTES = 250L * 1024L * 1024L
private const val UPDATE_BUFFER_SIZE = 64 * 1024
private const val UPDATE_PERCENT = 100L
