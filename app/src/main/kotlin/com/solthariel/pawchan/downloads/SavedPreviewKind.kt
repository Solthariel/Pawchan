package com.solthariel.pawchan.downloads

import java.io.IOException
import java.util.concurrent.CancellationException

internal enum class SavedPreviewKind { VIDEO, IMAGE, OTHER }

internal fun savedPreviewKind(
    name: String,
    mime: String? = null,
): SavedPreviewKind {
    val type =
        mime
            .orEmpty()
            .substringBefore(';')
            .trim()
            .lowercase()
    val extension = name.substringAfterLast('.', "").lowercase()
    return when {
        type.startsWith("video/") -> SavedPreviewKind.VIDEO
        type.startsWith("image/") -> SavedPreviewKind.IMAGE
        extension in SAVED_VIDEOS -> SavedPreviewKind.VIDEO
        extension in SAVED_IMAGES -> SavedPreviewKind.IMAGE
        else -> SavedPreviewKind.OTHER
    }
}

/** Provider thumbnails are optional. A denied grant is not a reason to try another access path. */
internal fun <T> savedThumbnailOrLocal(
    provider: () -> T?,
    local: () -> T?,
): T? =
    try {
        thumbnailAttempt(provider) ?: thumbnailAttempt(local)
    } catch (_: SecurityException) {
        null
    }

/** A platform codec can report malformed media as a plain RuntimeException. */
internal fun <T> thumbnailAttempt(action: () -> T?): T? =
    runCatching(action).getOrElse { error ->
        when (error) {
            is CancellationException, is SecurityException -> throw error
            is IOException, is IllegalArgumentException -> null
            is IllegalStateException, is UnsupportedOperationException -> null
            else -> if (error.javaClass == RuntimeException::class.java) null else throw error
        }
    }

/** Keep source aspect even for very tall images. No full-resolution bitmap just to draw one row. */
internal fun savedThumbnailSize(
    width: Int,
    height: Int,
    edge: Int,
): Pair<Int, Int>? {
    if (width <= 0 || height <= 0 || edge <= 0) return null
    val longest = maxOf(width, height)
    val limit = minOf(longest, edge).toLong()
    return maxOf(1, (width * limit / longest).toInt()) to maxOf(1, (height * limit / longest).toInt())
}

private val SAVED_VIDEOS = setOf("mp4", "webm", "mov", "mkv", "m4v", "3gp", "3gpp", "avi", "ogv")
private val SAVED_IMAGES = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "avif", "bmp")
