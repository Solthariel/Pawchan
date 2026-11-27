package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageMark
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftStroke

/** Reuse pixels only with identical geometry and an unchanged prefix of already rendered marks. */
internal fun imagePreviewContinuation(
    base: DraftImageEdit,
    target: DraftImageEdit,
): List<DraftImageMark>? {
    val sameGeometry = base.copy(marks = target.marks) == target
    val samePrefix =
        base.marks.size <= target.marks.size && base.marks.indices.all { base.marks[it] == target.marks[it] }
    if (!sameGeometry || !samePrefix) return null
    val pending = target.marks.drop(base.marks.size)
    return pending.takeIf { marks -> marks.all { it is DraftStroke || it is DraftShape || it is DraftRedaction } }
}

/** Every editor tool works on the full plane; the saved crop is applied on export. */
internal fun imageEditorDisplayEdit(edit: DraftImageEdit): DraftImageEdit = edit.copy(crop = DraftCrop())
