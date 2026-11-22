package com.solthariel.pawchan.ffmpeg

/** Decides how many verified bytes can be consumed, independently of sparse-file length. */
internal object PublishedRanges {
    /** Read error for a negative position; the JNI reader reports it as cancellation/error. */
    const val INVALID_POSITION = -2L

    /** Real end of the published input. */
    const val END_OF_INPUT = -1L

    @JvmStatic
    fun available(
        at: Long,
        prefix: Long,
        tailStart: Long,
        tailEnd: Long,
        total: Long,
    ): Long =
        when {
            at < 0 -> {
                INVALID_POSITION
            }

            total >= 0 && at >= total -> {
                END_OF_INPUT
            }

            else -> {
                var end = if (at < prefix) prefix else at
                if (at >= tailStart && at < tailEnd) end = maxOf(end, tailEnd)
                if (total >= 0) end = minOf(end, total)
                maxOf(0, end - at)
            }
        }
}
