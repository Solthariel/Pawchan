package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit

/** Eraser uses the same EXIF-oriented source geometry as the raster renderer. */
internal class ImageEraserGesture(
    initial: DraftImageEdit,
    sourceWidth: Int,
    sourceHeight: Int,
) {
    private val geometry = imageEditDimensions(sourceWidth, sourceHeight, initial, null)
    private val space = ImageEditRasterSpace(sourceWidth, sourceHeight, geometry.crop, initial)
    var current: DraftImageEdit = initial
        private set

    fun erase(
        x: Float,
        y: Float,
        radius: Float,
    ): DraftImageEdit {
        require(x.isFinite() && y.isFinite() && radius.isFinite() && radius >= 0f)
        val point = ImageEditPoint(x * geometry.width, y * geometry.height)
        val pixels = radius * minOf(geometry.width, geometry.height)
        current = eraseImageDrawing(current, space, point, pixels)
        return current
    }
}
