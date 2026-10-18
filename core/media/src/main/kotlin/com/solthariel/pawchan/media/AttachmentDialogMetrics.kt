package com.solthariel.pawchan.media

import kotlin.math.roundToInt

internal data class AttachmentDialogMetrics(
    val mediaHeight: Int,
    val footerHeight: Int,
    val totalHeight: Int,
)

/** Fit the file's aspect ratio, bounded by the space remaining after actual controls. */
internal fun attachmentDialogMetrics(
    availableHeight: Int,
    headerHeight: Int,
    actualFooterHeight: Int,
    gap: Int,
    mediaWidth: Int,
    aspectRatio: Float,
): AttachmentDialogMetrics {
    require(listOf(availableHeight, headerHeight, actualFooterHeight, gap, mediaWidth).all { it >= 0 })
    require(aspectRatio > 0 && aspectRatio.isFinite())
    val budget = (availableHeight - headerHeight - gap * 2).coerceAtLeast(0)
    val footer = actualFooterHeight.coerceAtMost(budget)
    val room = (budget - footer).coerceAtLeast(0)
    val media = minOf(mediaWidth.toDouble() / aspectRatio, room.toDouble()).roundToInt()
    val total = (headerHeight + media + footer + gap * 2).coerceAtMost(availableHeight)
    return AttachmentDialogMetrics(media, footer, total)
}
