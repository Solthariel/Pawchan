package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.solthariel.pawchan.domain.drafts.DraftImageEdit

/** Produces a bounded preview with the same renderer as export, never returning the source bitmap. */
internal fun renderImageEditPreview(
    bytes: ByteArray,
    edit: DraftImageEdit,
): Bitmap {
    require(isEditableStillImage(bytes)) { "Редактирование доступно только для неподвижных изображений" }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Не удалось определить размер изображения" }
    val options =
        BitmapFactory.Options().apply {
            inSampleSize = imageEditDecodeSample(bounds.outWidth, bounds.outHeight, PREVIEW_PIXEL_LIMIT)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    val decoded = requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options))
    try {
        require(decoded.width.toLong() * decoded.height <= PREVIEW_PIXEL_LIMIT)
        val oriented = orientBitmap(decoded, imageOrientation(bytes))
        try {
            return rasterizeImageEdit(oriented, edit)
        } finally {
            if (oriented !== decoded) oriented.recycle()
        }
    } finally {
        decoded.recycle()
    }
}

private const val PREVIEW_PIXEL_LIMIT = 1_500_000L
