package com.solthariel.pawchan.ffmpeg

import io.github.yuroyami.libmpvkt.stream.MpvStream
import io.github.yuroyami.libmpvkt.stream.MpvStreamProvider
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/** One source, one active mpv reader. The playback session owns and closes the source. */
class MpvProgressiveProvider(
    private val file: File,
    private val source: ProgressiveSource?,
    private val waiting: Runnable,
    private val resumed: Runnable,
) : MpvStreamProvider,
    AutoCloseable {
    private val gate = Any()
    private var active: MpvPublishedStream? = null
    private var closed = false

    override fun open(uri: String): MpvStream? =
        synchronized(gate) {
            if (closed || active != null) return@synchronized null
            val input = PublishedInput(waiting, resumed)
            source?.bind(input)
            try {
                input.open(file, source)
                MpvPublishedStream(input, ::release).also { active = it }
            } catch (_: IOException) {
                source?.bind(null)
                input.close()
                null
            }
        }

    private fun release(stream: MpvPublishedStream) {
        synchronized(gate) {
            if (active === stream) {
                source?.bind(null)
                active = null
            }
        }
    }

    override fun close() {
        val stream =
            synchronized(gate) {
                closed = true
                active
            }
        // mpv owns the stream lifetime. Cancel, but do not close a reader during an active read.
        stream?.cancel()
    }
}

/** Methods except cancel are serialized by mpv's stream callback contract. */
internal class MpvPublishedStream(
    private val input: PublishedInput,
    private val release: (MpvPublishedStream) -> Unit,
) : MpvStream {
    private var position = 0L
    private val scratch = ByteArray(STREAM_SCRATCH_BYTES)
    override val isSeekable: Boolean = true

    override fun read(into: ByteBuffer): Int {
        if (!into.hasRemaining()) return 0
        val count = input.read(position, scratch, minOf(into.remaining(), scratch.size))
        if (count > 0) {
            into.put(scratch, 0, count)
            position += count
        }
        return if (count == -1) {
            0
        } else if (count < 0) {
            -1
        } else {
            count
        }
    }

    override fun seek(offset: Long): Long =
        if (input.interrupted || offset < 0) {
            -1
        } else {
            val length = input.length()
            if (length >= 0 && offset > length) {
                -1
            } else {
                position = offset
                position
            }
        }

    override fun size(): Long = input.length()

    override fun cancel() = input.interrupt()

    override fun close() {
        input.interrupt()
        release(this)
        input.close()
    }
}

private const val STREAM_SCRATCH_BYTES = 32 * 1024
