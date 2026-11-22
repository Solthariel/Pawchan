package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit

internal data class ImagePreviewTransform(
    val quarterTurns: Int,
    val mirrored: Boolean,
)

/** Only transform a completed frame when its crop and all rendered marks are unchanged. */
internal fun imagePreviewTransform(
    base: DraftImageEdit,
    target: DraftImageEdit,
): ImagePreviewTransform? {
    if (base.crop != target.crop || base.marks != target.marks) return null
    val mirrored = base.mirrored != target.mirrored
    val baseTurns = if (mirrored) -base.quarterTurns else base.quarterTurns
    return ImagePreviewTransform((target.quarterTurns - baseTurns + QUARTER_TURNS) % QUARTER_TURNS, mirrored)
}

private const val QUARTER_TURNS = 4
