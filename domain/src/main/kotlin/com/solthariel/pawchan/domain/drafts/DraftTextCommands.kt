package com.solthariel.pawchan.domain.drafts

/** Adds one text mark at the tapped output position or returns null for invalid input or exhausted limits. */
fun DraftImageEdit.addTextMark(
    position: ImageCanvasPoint,
    text: String,
    appearance: DraftTextAppearance,
): DraftImageEdit? {
    val validText = text.isNotBlank() && text.length <= MAX_IMAGE_TEXT_LENGTH
    val validPoint = position.x.isFinite() && position.y.isFinite() && position.x in 0f..1f && position.y in 0f..1f
    val roomForMark = marks.size < MAX_IMAGE_MARKS && marks.sumOf { it.pointCount } < MAX_IMAGE_POINTS
    return if (validText && validPoint && roomForMark) {
        canvasToSource(position)?.let { anchor -> copy(marks = marks + DraftTextMark(text, anchor, appearance)) }
    } else {
        null
    }
}

/** Only text is removed: the index must refer to a current text mark. */
fun DraftImageEdit.removeTextMark(index: Int): DraftImageEdit {
    if (marks.getOrNull(index) !is DraftTextMark) return this
    return copy(marks = marks.filterIndexed { position, _ -> position != index })
}
