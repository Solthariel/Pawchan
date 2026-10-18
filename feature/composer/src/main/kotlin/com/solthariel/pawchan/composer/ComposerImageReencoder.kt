package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import com.solthariel.pawchan.data.network.PostingMediaBytes
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt

internal fun reencodeAttachmentImage(
    content: ByteArray,
    settings: ImageReencoding,
): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(content, 0, content.size, bounds)
    val width = (bounds.outWidth * settings.scaleFactor).roundToInt().coerceAtLeast(1)
    val height = (bounds.outHeight * settings.scaleFactor).roundToInt().coerceAtLeast(1)
    require(bounds.outWidth > 0 && bounds.outHeight > 0 && width.toLong() * height <= MAX_REENCODE_PIXELS) {
        "Изображение слишком большое для перекодирования: уменьшите масштаб"
    }
    val decode = BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds, width, height) }
    val bitmap =
        requireNotNull(BitmapFactory.decodeByteArray(content, 0, content.size, decode)) {
            "Не удалось декодировать изображение"
        }
    val resized = bitmap.scale(width, height)
    if (resized !== bitmap) bitmap.recycle()
    val oriented = orientBitmap(resized, imageOrientation(content))
    if (oriented !== resized) resized.recycle()
    return try {
        ByteArrayOutputStream().use { output ->
            val format = if (settings.jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
            require(oriented.compress(format, settings.quality, output)) { "Не удалось перекодировать изображение" }
            output.toByteArray()
        }
    } finally {
        oriented.recycle()
    }
}

internal fun imageOrientation(content: ByteArray): Int =
    if (PostingMediaBytes.format(content) in setOf("jpg", "png", "webp")) {
        try {
            ExifInterface(ByteArrayInputStream(content)).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1).takeIf {
                it in
                    1..ExifInterface.ORIENTATION_ROTATE_270
            }
                ?: 1
        } catch (_: IOException) {
            1
        }
    } else {
        1
    }

private fun sampleFor(
    bounds: BitmapFactory.Options,
    width: Int,
    height: Int,
): Int {
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= width && bounds.outHeight / (sample * 2) >= height) sample *= 2
    while (bounds.outWidth.toLong() * bounds.outHeight / sample / sample > MAX_REENCODE_PIXELS) sample *= 2
    return sample
}

private fun orientBitmap(
    bitmap: Bitmap,
    orientation: Int,
): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
            matrix.setScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_180 -> {
            matrix.setRotate(HALF_ROTATION)
        }

        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setScale(1f, -1f)
        }

        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(QUARTER_ROTATION)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_90 -> {
            matrix.setRotate(QUARTER_ROTATION)
        }

        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-QUARTER_ROTATION)
            matrix.postScale(-1f, 1f)
        }

        ExifInterface.ORIENTATION_ROTATE_270 -> {
            matrix.setRotate(-QUARTER_ROTATION)
        }
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private const val MAX_REENCODE_PIXELS = 8_000_000L

private const val HALF_ROTATION = 180f
private const val QUARTER_ROTATION = 90f
