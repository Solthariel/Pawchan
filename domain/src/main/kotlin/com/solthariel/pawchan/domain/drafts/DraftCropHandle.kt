package com.solthariel.pawchan.domain.drafts

/** Handles are named in the displayed, rotated image, not the source coordinate system. */
enum class DraftCropHandle(
    val horizontal: Int,
    val vertical: Int,
) {
    LEFT(-1, 0),
    RIGHT(1, 0),
    TOP(0, -1),
    BOTTOM(0, 1),
    TOP_LEFT(-1, -1),
    TOP_RIGHT(1, -1),
    BOTTOM_LEFT(-1, 1),
    BOTTOM_RIGHT(1, 1),
}

data class CanvasCrop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** The crop editor displays the entire transformed source, including the area outside the crop. */
fun DraftImageEdit.cropOnFullCanvas(): CanvasCrop {
    val full = copy(crop = DraftCrop())
    val a = full.sourceToCanvas(DraftPoint(crop.left, crop.top))
    val b = full.sourceToCanvas(DraftPoint(crop.right, crop.bottom))
    return CanvasCrop(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y))
}

internal fun DraftImageEdit.withCanvasCrop(bounds: CanvasCrop): DraftImageEdit {
    val full = copy(crop = DraftCrop())
    val a = requireNotNull(full.canvasToSource(ImageCanvasPoint(bounds.left, bounds.top)))
    val b = requireNotNull(full.canvasToSource(ImageCanvasPoint(bounds.right, bounds.bottom)))
    return copy(crop = DraftCrop(minOf(a.x, b.x), minOf(a.y, b.y), maxOf(a.x, b.x), maxOf(a.y, b.y)))
}

/** Drag a handle on the full transformed source; aspect is width/height in the displayed output. */
fun DraftImageEdit.resizeCrop(
    handle: DraftCropHandle,
    target: ImageCanvasPoint,
    sourceWidth: Int,
    sourceHeight: Int,
    aspect: Float? = null,
): DraftImageEdit {
    require(sourceWidth > 0 && sourceHeight > 0 && target.x.isFinite() && target.y.isFinite())
    require(aspect == null || (aspect.isFinite() && aspect in MIN_CROP_RATIO..MAX_CROP_RATIO))
    val width = if (quarterTurns % 2 == 0) sourceWidth.toFloat() else sourceHeight.toFloat()
    val height = if (quarterTurns % 2 == 0) sourceHeight.toFloat() else sourceWidth.toFloat()
    val size = CropCanvasSize(width, height)
    val bounds = cropOnFullCanvas()
    val point = ImageCanvasPoint(target.x.coerceIn(0f, 1f), target.y.coerceIn(0f, 1f))
    val resized =
        if (aspect == null) {
            freeCropResize(bounds, handle, point, size)
        } else {
            aspectCropResize(bounds, handle, point, size, aspect)
        }
    if (resized == null || resized.left >= resized.right || resized.top >= resized.bottom) return this
    return withCanvasCrop(resized)
}

internal data class CropCanvasSize(
    val width: Float,
    val height: Float,
)

private const val MIN_CROP_RATIO = 0.001f
private const val MAX_CROP_RATIO = 1000f
