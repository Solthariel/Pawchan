package com.solthariel.pawchan.ffmpeg

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile

private const val COPY_BUFFER_BYTES = 64 * 1024

/**
 * Writes a response body at [start]. [written] receives the previous and the new end after every chunk,
 * so readers only see bytes that are actually on disk. Copying stops quietly when [live] becomes false.
 */
@Throws(IOException::class)
internal fun copyBody(
    input: InputStream,
    target: File,
    start: Long,
    expectedEnd: Long,
    live: () -> Boolean,
    written: (Long, Long) -> Unit,
) {
    input.use { stream ->
        RandomAccessFile(target, "rw").use { output ->
            output.seek(start)
            val end = copyChunks(stream, output, start, expectedEnd, live, written)
            if (live() && expectedEnd >= 0 && end != expectedEnd) throw IOException("Truncated response body")
        }
    }
}

@Throws(IOException::class)
private fun copyChunks(
    stream: InputStream,
    output: RandomAccessFile,
    start: Long,
    expectedEnd: Long,
    live: () -> Boolean,
    written: (Long, Long) -> Unit,
): Long {
    val buffer = ByteArray(COPY_BUFFER_BYTES)
    var position = start
    var count = stream.read(buffer)
    while (count != -1 && live()) {
        if (count > 0) {
            checkChunk(position, count, expectedEnd)
            output.write(buffer, 0, count)
            written(position, position + count)
            position += count
        }
        count = stream.read(buffer)
    }
    return position
}

@Throws(IOException::class)
private fun checkChunk(
    position: Long,
    count: Int,
    expectedEnd: Long,
) {
    val overLimit = position > MAX_MEDIA_BYTES - count
    val overDeclared = expectedEnd >= 0 && position + count > expectedEnd
    if (overLimit || overDeclared) throw IOException("Video response exceeds declared limit")
}
