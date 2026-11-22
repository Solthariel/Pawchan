package com.solthariel.pawchan.domain.drafts

/** Output-space coordinates may lie outside 0..1 when a mark is partially cropped. */
data class ImageCanvasPoint(
    val x: Float,
    val y: Float,
)

fun DraftImageEdit.sourceToCanvas(point: DraftPoint): ImageCanvasPoint {
    var x = (point.x - crop.left) / (crop.right - crop.left)
    val y = (point.y - crop.top) / (crop.bottom - crop.top)
    if (mirrored) x = 1f - x
    return when (quarterTurns) {
        0 -> ImageCanvasPoint(x, y)
        1 -> ImageCanvasPoint(1f - y, x)
        2 -> ImageCanvasPoint(1f - x, 1f - y)
        else -> ImageCanvasPoint(y, 1f - x)
    }
}

fun DraftImageEdit.canvasToSource(point: ImageCanvasPoint): DraftPoint? {
    val unrotated =
        when (quarterTurns) {
            0 -> point
            1 -> ImageCanvasPoint(point.y, 1f - point.x)
            2 -> ImageCanvasPoint(1f - point.x, 1f - point.y)
            else -> ImageCanvasPoint(1f - point.y, point.x)
        }
    val x = if (mirrored) 1f - unrotated.x else unrotated.x
    val sourceX = crop.left + x * (crop.right - crop.left)
    val sourceY = crop.top + unrotated.y * (crop.bottom - crop.top)
    return if (sourceX in 0f..1f && sourceY in 0f..1f) {
        DraftPoint(sourceX, sourceY)
    } else {
        null
    }
}
