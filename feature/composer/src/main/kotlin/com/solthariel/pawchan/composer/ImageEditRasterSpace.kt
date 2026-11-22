package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint

/** Source-space normalized points become output-space pixels after the crop and D4 transform. */
internal class ImageEditRasterSpace(
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val crop: ImageEditBounds,
    private val edit: DraftImageEdit,
) {
    private val width = (crop.right - crop.left).toFloat()
    private val height = (crop.bottom - crop.top).toFloat()
    val shortSide: Float = minOf(sourceWidth, sourceHeight).toFloat()

    fun map(point: DraftPoint): ImageEditPoint {
        val local = point.x * sourceWidth - crop.left
        val x = if (edit.mirrored) width - local else local
        val y = point.y * sourceHeight - crop.top
        return when (edit.quarterTurns) {
            0 -> ImageEditPoint(x, y)
            1 -> ImageEditPoint(height - y, x)
            2 -> ImageEditPoint(width - x, height - y)
            else -> ImageEditPoint(y, width - x)
        }
    }
}
