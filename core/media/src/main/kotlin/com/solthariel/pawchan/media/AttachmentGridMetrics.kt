package com.solthariel.pawchan.media

import kotlin.math.min

data class AttachmentGridMetrics(
    val rows: Int,
    val cellSize: Float,
)

fun calculateAttachmentGridMetrics(
    itemCount: Int,
    availableWidth: Float,
    availableHeight: Float,
    maximumCellSize: Float,
    gap: Float,
): AttachmentGridMetrics {
    val rows = (itemCount.coerceAtLeast(1) + ATTACHMENT_GRID_COLUMNS - 1) / ATTACHMENT_GRID_COLUMNS
    val widthBound = (availableWidth - gap) / ATTACHMENT_GRID_COLUMNS
    val heightBound = (availableHeight - gap * (rows - 1)) / rows
    return AttachmentGridMetrics(
        rows = rows,
        cellSize = min(maximumCellSize, min(widthBound, heightBound)).coerceAtLeast(0f),
    )
}

private const val ATTACHMENT_GRID_COLUMNS = 2
