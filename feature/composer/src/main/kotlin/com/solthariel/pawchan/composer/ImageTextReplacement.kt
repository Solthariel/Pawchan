package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftTextMark

internal fun DraftImageEdit.replaceText(
    index: Int,
    text: DraftTextMark,
): DraftImageEdit {
    if (marks.getOrNull(index) !is DraftTextMark) return this
    return copy(marks = marks.mapIndexed { position, mark -> if (position == index) text else mark })
}
