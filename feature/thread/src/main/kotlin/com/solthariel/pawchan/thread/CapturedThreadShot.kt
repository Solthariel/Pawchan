package com.solthariel.pawchan.thread

import android.graphics.Bitmap
import java.io.IOException

/** Immutable membership, exact screen pixels. Bitmaps are borrowed read-only for the lifetime of this request. */
class CapturedThreadShot(
    rows: List<Bitmap>,
) {
    val strips: List<Bitmap> = rows.toList()
    val width: Int = strips.firstOrNull()?.width ?: 0
    val height: Int

    init {
        val badStrip = strips.any { it.isRecycled || it.width != width || it.height <= 0 }
        if (width <= 0 || badStrip) throw IOException(SHOT_MISSING)
        val total = strips.sumOf { it.height.toLong() }
        if (total > THREAD_SHOT_MAX_PIXELS / width) throw IOException(SHOT_TOO_LARGE)
        height = total.toInt()
    }
}
