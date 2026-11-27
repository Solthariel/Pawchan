package com.solthariel.pawchan.ffmpeg

import java.nio.ByteBuffer

/** A read-only view for libmpv. The saved document and its URLs/range offsets stay byte-for-byte untouched. */
internal class Mp4StreamTimeline(
    private val input: PublishedInput,
) {
    private var inspected: List<Mp4Overlay>? = null

    fun read(
        position: Long,
        target: ByteArray,
        length: Int,
    ): Int {
        val patches = inspected ?: inspect().also { inspected = it }
        val count = input.read(position, target, length)
        if (count <= 0) return count
        for (patch in patches) {
            val start = maxOf(position, patch.offset)
            val end = minOf(position + count, patch.offset + patch.bytes.size)
            if (end > start) {
                patch.bytes.copyInto(
                    target,
                    (start - position).toInt(),
                    (start - patch.offset).toInt(),
                    (end - patch.offset).toInt(),
                )
            }
        }
        return count
    }

    private fun inspect(): List<Mp4Overlay> {
        val total = input.length()
        val limit = if (total >= 0) total else MAX_MEDIA_BYTES
        val header = ByteArray(EXTENDED_HEADER)
        var start = 0L
        var inspectedBoxes = 1
        var box = nextBox(start, limit, total >= 0, header)
        while (box != null && box.kind != "moov" && inspectedBoxes < MAX_TOP_LEVEL) {
            start += box.size
            box = nextBox(start, limit - start, total >= 0, header)
            inspectedBoxes++
        }
        return if (box != null && box.kind == "moov") inspectMovie(start, box) else emptyList()
    }

    private fun nextBox(
        start: Long,
        remaining: Long,
        knownSize: Boolean,
        header: ByteArray,
    ): TopBox? {
        if (remaining < BOX_HEADER || !readFully(start, header, BOX_HEADER)) return null
        val data = ByteBuffer.wrap(header)
        val type = data.fourCc(Int.SIZE_BYTES)
        val validStart = start != 0L || type == "ftyp"
        val bounded = knownSize || data.getInt(0) != 0
        return if (validStart && bounded && type != "moof") topBox(start, remaining, data, type) else null
    }

    private fun topBox(
        start: Long,
        remaining: Long,
        data: ByteBuffer,
        type: String,
    ): TopBox? {
        val raw = Integer.toUnsignedLong(data.getInt(0))
        val width = if (raw == 1L) EXTENDED_HEADER else BOX_HEADER
        val size =
            when (raw) {
                0L -> {
                    remaining
                }

                1L -> {
                    if (remaining < width || !readFully(start, data.array(), width)) return null
                    data.getLong(BOX_HEADER)
                }

                else -> {
                    raw
                }
            }
        return if (size in width.toLong()..remaining) TopBox(type, width, size) else null
    }

    private fun inspectMovie(
        start: Long,
        box: TopBox,
    ): List<Mp4Overlay> {
        val payload = box.size - box.header
        if (payload !in 1L..MAX_MOVIE_BYTES.toLong()) return emptyList()
        val body = ByteArray(payload.toInt())
        val offset = start + box.header
        return if (readFully(offset, body, body.size)) mp4ClipTimeline(body, offset) else emptyList()
    }

    private fun readFully(
        position: Long,
        target: ByteArray,
        length: Int,
    ): Boolean {
        var done = 0
        while (done < length) {
            val count = input.read(position + done, target, length - done, offset = done)
            if (count <= 0) return false
            done += count
        }
        return true
    }
}

private data class TopBox(
    val kind: String,
    val header: Int,
    val size: Long,
)

private const val MAX_TOP_LEVEL = 32
private const val MAX_MOVIE_BYTES = 512 * 1024
