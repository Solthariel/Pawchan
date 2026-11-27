package com.solthariel.pawchan.ffmpeg

import java.nio.ByteBuffer
import kotlin.math.abs

/** A field is always in movie ticks; media timestamps and the composition/sample tables are not modified. */
internal data class MovieTicks(
    val offset: Int,
    val width: Int,
    val value: Long,
) {
    fun subtract(
        common: Long,
        base: Long,
    ): Mp4Overlay {
        val number = value - common
        val bytes = ByteBuffer.allocate(width)
        if (width == Long.SIZE_BYTES) bytes.putLong(number) else bytes.putInt(number.toInt())
        return Mp4Overlay(base + offset, bytes.array())
    }
}

internal data class Mp4Overlay(
    val offset: Long,
    val bytes: ByteArray,
)

private data class TrackEdits(
    val kind: String,
    val duration: MovieTicks,
    val gap: MovieTicks,
)

/** Only common, dominant leading empty edits in short A/V clips are candidates. Complex edits pass unchanged. */
internal fun mp4ClipTimeline(
    movie: ByteArray,
    fileOffset: Long,
): List<Mp4Overlay> =
    try {
        clipEdits(movie, fileOffset)
    } catch (_: IllegalArgumentException) {
        // Unrecognized edits are an ordinary file, not a playback error.
        emptyList()
    }

private fun clipEdits(
    movie: ByteArray,
    fileOffset: Long,
): List<Mp4Overlay> {
    val data = ByteBuffer.wrap(movie)
    val boxes = data.boxes(0, movie.size)
    val header = requireNotNull(boxes.singleOrNull { it.type == "mvhd" })
    val duration = data.headerDuration(header)
    val scale = Integer.toUnsignedLong(data.getInt(duration.offset - Int.SIZE_BYTES))
    val trackBoxes = boxes.filter { it.type == "trak" }
    require(scale > 0L && boxes.none { it.type == "mvex" } && trackBoxes.size in 1..MAX_CLIP_TRACKS)
    val tracks = trackBoxes.map(data::trackEdits)
    require(tracks.any { it.kind == "vide" })
    val gap = tracks.minOf { it.gap.value }
    val longest = tracks.maxOf { it.duration.value }
    val span = duration.value - gap
    require(abs(duration.value - longest) <= 1 && span > 0)
    require(gap / scale >= MIN_PADDING_SECONDS && gap / span >= MIN_GAP_SPANS)
    return (listOf(duration) + tracks.flatMap { listOf(it.duration, it.gap) }).map { it.subtract(gap, fileOffset) }
}

private fun ByteBuffer.trackEdits(track: Mp4Box): TrackEdits {
    val children = boxes(track.start, track.end)
    val duration = headerDuration(requireNotNull(children.singleOrNull { it.type == "tkhd" }))
    val mdia = requireNotNull(children.singleOrNull { it.type == "mdia" })
    val hdlr = requireNotNull(boxes(mdia.start, mdia.end).singleOrNull { it.type == "hdlr" })
    require(hdlr.end - hdlr.start >= HANDLER_END)
    val kind = fourCc(hdlr.start + HANDLER_OFFSET)
    require(kind == "vide" || kind == "soun")
    val edts = requireNotNull(children.singleOrNull { it.type == "edts" })
    val edits = requireNotNull(boxes(edts.start, edts.end).singleOrNull())
    require(edits.type == "elst")
    val gap = simpleLeadingGap(edits, duration.value)
    return TrackEdits(kind, duration, gap)
}

private fun ByteBuffer.simpleLeadingGap(
    edits: Mp4Box,
    duration: Long,
): MovieTicks {
    val width = requireNotNull(fieldWidth(edits))
    val entrySize = width * 2 + Int.SIZE_BYTES
    require(edits.end - edits.start == BOX_HEADER + entrySize * 2 && getInt(edits.start + Int.SIZE_BYTES) == 2)
    val first = edits.start + BOX_HEADER
    val second = first + entrySize
    require(signed(first + width, width) == -1L && signed(second + width, width) >= 0L)
    require(getInt(first + width * 2) == NORMAL_RATE && getInt(second + width * 2) == NORMAL_RATE)
    val gap = ticks(first, width)
    val media = ticks(second, width).value
    require(gap.value in 1 until duration && media in 1..MAX_TICKS)
    require(abs(duration - gap.value - media) <= 1)
    return gap
}

private fun ByteBuffer.headerDuration(header: Mp4Box): MovieTicks {
    val width = requireNotNull(fieldWidth(header))
    val base = header.start + Int.SIZE_BYTES + 2 * width
    val offset = base + if (header.type == "mvhd") Int.SIZE_BYTES else 2 * Int.SIZE_BYTES
    require(offset + width <= header.end)
    return ticks(offset, width).also { require(it.value in 1..MAX_TICKS) }
}

private fun ByteBuffer.fieldWidth(header: Mp4Box): Int? =
    if (header.end - header.start < Int.SIZE_BYTES) {
        null
    } else {
        when (get(header.start).toInt()) {
            0 -> Int.SIZE_BYTES
            1 -> Long.SIZE_BYTES
            else -> null
        }
    }

private fun ByteBuffer.signed(
    offset: Int,
    width: Int,
): Long = if (width == Long.SIZE_BYTES) getLong(offset) else getInt(offset).toLong()

private fun ByteBuffer.ticks(
    offset: Int,
    width: Int,
): MovieTicks {
    val value = if (width == Long.SIZE_BYTES) getLong(offset) else Integer.toUnsignedLong(getInt(offset))
    return MovieTicks(offset, width, value)
}

private const val MIN_PADDING_SECONDS = 30L
private const val MIN_GAP_SPANS = 4
private const val MAX_CLIP_TRACKS = 8
private const val HANDLER_OFFSET = 8
private const val HANDLER_END = 12
private const val NORMAL_RATE = 0x00010000
private const val MAX_TICKS = Long.MAX_VALUE / 4
