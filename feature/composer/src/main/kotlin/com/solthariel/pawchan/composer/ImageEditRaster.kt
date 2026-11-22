package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withMatrix
import com.solthariel.pawchan.domain.drafts.DraftImageEdit

/** Renders into a new bitmap; the input is EXIF-oriented and remains owned by the caller. */
internal fun rasterizeImageEdit(
    source: Bitmap,
    edit: DraftImageEdit,
): Bitmap {
    require(!source.isRecycled && source.width > 0 && source.height > 0)
    val dimensions = imageEditDimensions(source.width, source.height, edit, null)
    val bounds = dimensions.crop
    val output = createBitmap(dimensions.width, dimensions.height)
    var completed = false
    try {
        val canvas = Canvas(output)
        canvas.withMatrix(imageEditMatrix(bounds, edit)) {
            drawBitmap(
                source,
                Rect(bounds.left, bounds.top, bounds.right, bounds.bottom),
                RectF(0f, 0f, (bounds.right - bounds.left).toFloat(), (bounds.bottom - bounds.top).toFloat()),
                Paint(Paint.FILTER_BITMAP_FLAG),
            )
        }
        val space = ImageEditRasterSpace(source.width, source.height, bounds, edit)
        edit.marks.forEach { mark -> drawImageEditMark(canvas, output, space, mark) }
        completed = true
        return output
    } finally {
        if (!completed) output.recycle()
    }
}

private fun imageEditMatrix(
    bounds: ImageEditBounds,
    edit: DraftImageEdit,
): Matrix = Matrix().apply { setValues(imageEditMatrixValues(bounds, edit)) }
