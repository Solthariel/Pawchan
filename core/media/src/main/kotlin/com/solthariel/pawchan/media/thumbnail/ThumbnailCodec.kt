package com.solthariel.pawchan.media.thumbnail

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import okhttp3.ResponseBody
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/** Bound both encoded bytes and decoded pixels before allocating a bitmap. */
internal class ThumbnailCodec(
    density: Float,
) {
    private val shortSide = max(1, (THUMBNAIL_DP * density).roundToInt())
    private val longSide = shortSide * MAX_ASPECT_RATIO

    fun decode(body: ResponseBody): Bitmap {
        val bytes = readThumbnailBytes(body)
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Invalid thumbnail bounds")
        options.inSampleSize = thumbnailSampleSize(options.outWidth, options.outHeight, longSide)
        options.inJustDecodeBounds = false
        val decoded =
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: throw IOException("Invalid thumbnail image")
        return resize(decoded)
    }

    private fun resize(bitmap: Bitmap): Bitmap {
        val scale =
            minOf(
                1f,
                shortSide / minOf(bitmap.width, bitmap.height).toFloat(),
                longSide / maxOf(bitmap.width, bitmap.height).toFloat(),
            )
        if (scale >= 1f) return bitmap
        val resized =
            bitmap.scale(
                max(1, (bitmap.width * scale).roundToInt()),
                max(1, (bitmap.height * scale).roundToInt()),
            )
        if (resized !== bitmap) bitmap.recycle()
        return resized
    }

    fun hardware(bitmap: Bitmap): Bitmap =
        if (bitmap.config == Bitmap.Config.HARDWARE) {
            bitmap
        } else {
            val hardware = bitmap.copy(Bitmap.Config.HARDWARE, false)
            if (hardware != null) bitmap.recycle()
            hardware ?: bitmap
        }
}

private fun readThumbnailBytes(body: ResponseBody): ByteArray {
    if (body.contentLength() > MAX_ENCODED_BYTES) throw IOException("Thumbnail exceeds byte limit")
    return body.source().use { source ->
        source.request(MAX_ENCODED_BYTES + 1L)
        if (source.buffer.size > MAX_ENCODED_BYTES) throw IOException("Thumbnail exceeds byte limit")
        source.readByteArray()
    }
}

internal fun thumbnailSampleSize(
    width: Int,
    height: Int,
    maxSide: Int,
): Int {
    var sample = 1
    val largest = maxOf(width, height)
    while (largest / sample > maxSide && sample <= Int.MAX_VALUE / 2) sample *= 2
    return sample
}

private const val THUMBNAIL_DP = 96f
private const val MAX_ASPECT_RATIO = 4
private const val MAX_ENCODED_BYTES = 4L * 1024L * 1024L
