package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import com.solthariel.pawchan.data.network.PostingMediaBytes
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import java.io.ByteArrayOutputStream

/** Decoder and renderer operate on private bitmaps; caller-owned bytes are never modified. */
internal fun exportImageEdit(
    content: ByteArray,
    edit: DraftImageEdit,
    encoding: DraftImageReencoding?,
): ImageEditOutput {
    require(isEditableStillImage(content)) { "Редактирование доступно только для неподвижных изображений" }
    val sourceFormat = requireNotNull(PostingMediaBytes.format(content))
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(content, 0, content.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Не удалось определить размер изображения" }
    val options =
        BitmapFactory.Options().apply {
            inSampleSize = imageEditDecodeSample(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    val decoded =
        requireNotNull(BitmapFactory.decodeByteArray(content, 0, content.size, options)) {
            "Не удалось открыть изображение"
        }
    val rendered =
        try {
            require(decoded.width.toLong() * decoded.height <= IMAGE_EDIT_DECODE_PIXEL_LIMIT)
            val oriented = orientBitmap(decoded, imageOrientation(content))
            try {
                val dimensions = imageEditDimensions(oriented.width, oriented.height, edit, encoding)
                scaleEditedBitmap(rasterizeImageEdit(oriented, edit), dimensions)
            } finally {
                if (oriented !== decoded) oriented.recycle()
            }
        } finally {
            decoded.recycle()
        }
    return try {
        encodeEditedBitmap(rendered, sourceFormat, encoding)
    } finally {
        rendered.recycle()
    }
}

private fun scaleEditedBitmap(
    bitmap: Bitmap,
    dimensions: ImageEditDimensions,
): Bitmap {
    if (bitmap.width == dimensions.width && bitmap.height == dimensions.height) return bitmap
    return try {
        bitmap.scale(dimensions.width, dimensions.height)
    } finally {
        bitmap.recycle()
    }
}

private fun encodeEditedBitmap(
    bitmap: Bitmap,
    sourceFormat: String,
    encoding: DraftImageReencoding?,
): ImageEditOutput {
    val jpeg = imageEditIsJpeg(sourceFormat, encoding)
    val format = if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
    val quality = encoding?.quality ?: DEFAULT_EDIT_JPEG_QUALITY
    val bytes =
        ByteArrayOutputStream().use { output ->
            require(bitmap.compress(format, quality, output)) { "Не удалось сохранить изображение" }
            output.toByteArray()
        }
    return ImageEditOutput(bytes, if (jpeg) "jpg" else "png", if (jpeg) "image/jpeg" else "image/png")
}

private const val DEFAULT_EDIT_JPEG_QUALITY = 95
