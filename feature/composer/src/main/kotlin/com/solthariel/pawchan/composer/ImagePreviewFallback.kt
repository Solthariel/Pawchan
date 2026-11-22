package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageMark
import com.solthariel.pawchan.domain.drafts.DraftRedaction

/** Retain old pixels during undo/erase/reset; opaque target masks cover any newly hidden area. */
internal fun imagePreviewFallback(
    base: DraftImageEdit,
    target: DraftImageEdit,
): List<DraftImageMark>? {
    if (base.crop != target.crop) return null
    return target.marks.filterIsInstance<DraftRedaction>()
}

internal fun imagePreviewFallbackTransform(
    base: DraftImageEdit,
    target: DraftImageEdit,
): ImagePreviewTransform? = imagePreviewTransform(base.copy(marks = target.marks), target)
