package com.solthariel.pawchan.ffmpeg

import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException

/** Upper bound for one session-owned media file. */
internal const val MAX_MEDIA_BYTES = 1024L * 1024 * 1024

private const val HTTP_OK = 200
private const val HTTP_PARTIAL_CONTENT = 206
private const val HTTP_RANGE_NOT_SATISFIABLE = 416

/** Encoded HTTP bodies cannot be addressed using file byte offsets. */
internal fun mediaRequest(url: String): Request.Builder {
    val builder = Request.Builder().url(url)
    return builder.header("Accept-Encoding", "identity")
}

/** Entity identity from the sequential GET. Without it concurrent requests could merge different files. */
internal class EntityTag(
    private val etag: String?,
    private val lastModified: String?,
) {
    val ifRange: String? get() = etag ?: lastModified

    fun sameEntity(response: Response): Boolean {
        val current = if (etag != null) response.header("ETag") else response.header("Last-Modified")
        return current == null || current == ifRange
    }

    companion object {
        /** A weak ETag cannot validate byte ranges. */
        fun of(response: Response): EntityTag =
            EntityTag(response.header("ETag")?.takeUnless { it.startsWith("W/") }, response.header("Last-Modified"))
    }
}

@Throws(IOException::class)
internal fun checkedPrimaryBody(response: Response): ResponseBody {
    if (response.code != HTTP_OK) throw IOException("Video HTTP ${response.code}")
    if (!"identity".equals(response.header("Content-Encoding", "identity"), ignoreCase = true)) {
        throw IOException("Video server ignored identity encoding")
    }
    return response.body
}

/** 200 or 416 to a range request means the server will not serve parts of this entity. */
internal fun rangeRefused(response: Response): Boolean {
    val code = response.code
    return code == HTTP_OK || code == HTTP_RANGE_NOT_SATISFIABLE
}

/** Returns the exclusive end of a verified 206 body for [start]. */
@Throws(IOException::class)
internal fun checkedRangeEnd(
    response: Response,
    start: Long,
    total: Long,
    tag: EntityTag,
): Long {
    if (response.code != HTTP_PARTIAL_CONTENT) throw IOException("Range HTTP ${response.code}")
    val range = ResponseRange.parse(response.header("Content-Range"), start, total)
    val declared = response.body.contentLength()
    val consistent = tag.sameEntity(response) && (declared < 0 || declared == range.endExclusive - start)
    if (!consistent) throw IOException("Video changed or range body length mismatch")
    return range.endExclusive
}
