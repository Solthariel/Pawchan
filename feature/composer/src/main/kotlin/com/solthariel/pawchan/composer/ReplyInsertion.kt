package com.solthariel.pawchan.composer

internal fun appendReplyInsertion(
    draft: ComposerDraftState,
    insertion: String,
    limit: Int,
): ComposerDraftState? {
    val separator = if (draft.comment.isEmpty() || draft.comment.endsWith('\n')) "" else "\n"
    val text = draft.comment + separator + insertion
    return if (text.length <= limit) draft.copy(comment = text) else null
}
