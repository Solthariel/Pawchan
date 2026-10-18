package com.solthariel.pawchan.media.thumbnail

import android.graphics.Bitmap
import java.util.LinkedHashMap

internal class ThumbnailMemoryCache(
    private val maxEntries: Int,
    private val maxSizeBytes: Long,
) {
    private val values = LinkedHashMap<String, Bitmap>(0, LRU_LOAD_FACTOR, true)
    private var sizeBytes = 0L

    @Synchronized
    operator fun get(key: String): Bitmap? = values[key]

    @Synchronized
    fun put(
        key: String,
        bitmap: Bitmap,
    ) {
        values.put(key, bitmap)?.let { previous -> sizeBytes -= previous.safeAllocationByteCount() }
        sizeBytes += bitmap.safeAllocationByteCount()
        trim()
    }

    @Synchronized
    fun clear() {
        values.clear()
        sizeBytes = 0L
    }

    private fun trim() {
        val iterator = values.entries.iterator()
        while ((values.size > maxEntries || sizeBytes > maxSizeBytes) && iterator.hasNext()) {
            val entry = iterator.next()
            sizeBytes -= entry.value.safeAllocationByteCount()
            iterator.remove()
        }
    }
}

private fun Bitmap.safeAllocationByteCount(): Long =
    try {
        allocationByteCount.toLong()
    } catch (_: Exception) {
        byteCount.toLong()
    }

private const val LRU_LOAD_FACTOR = 0.75f
