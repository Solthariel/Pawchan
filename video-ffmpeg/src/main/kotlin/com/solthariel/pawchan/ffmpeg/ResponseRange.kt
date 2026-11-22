package com.solthariel.pawchan.ffmpeg

import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

/** Strict byte-range validation; a 200 response is never written at a nonzero offset. */
class ResponseRange private constructor(
    @JvmField val start: Long,
    @JvmField val endExclusive: Long,
    @JvmField val total: Long,
) {
    companion object {
        private val PATTERN = Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)")
        private const val START_GROUP = 1
        private const val END_GROUP = 2
        private const val TOTAL_GROUP = 3

        @JvmStatic
        @Throws(IOException::class)
        fun parse(
            header: String?,
            requested: Long,
            knownTotal: Long,
        ): ResponseRange {
            val matcher = PATTERN.matcher(header ?: "")
            if (!matcher.matches()) throw IOException("Invalid Content-Range")
            val start = matcher.bound(START_GROUP)
            val end = matcher.bound(END_GROUP)
            val total = matcher.bound(TOTAL_GROUP)
            if (!matchesRequest(start, requested) || !withinTotal(start, end, total, knownTotal)) {
                throw IOException("Mismatched Content-Range")
            }
            return ResponseRange(start, end + 1, total)
        }

        @Throws(IOException::class)
        private fun Matcher.bound(group: Int): Long =
            try {
                group(group).orEmpty().toLong()
            } catch (exception: NumberFormatException) {
                throw IOException("Content-Range overflow", exception)
            }

        private fun matchesRequest(
            start: Long,
            requested: Long,
        ): Boolean = start == requested

        /** The inclusive end must fit inside the entity and leave room for an exclusive end. */
        private fun withinTotal(
            start: Long,
            end: Long,
            total: Long,
            knownTotal: Long,
        ): Boolean {
            val ordered = end in start until total
            val representable = end != Long.MAX_VALUE
            val sameEntity = knownTotal < 0 || total == knownTotal
            return ordered && representable && sameEntity
        }
    }
}
