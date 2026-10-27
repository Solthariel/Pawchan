package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress

/** Prepared per rule revision, evaluated outside the UI thread. */
class ContentFilterEngine(
    rules: List<ContentFilterRule>,
    place: ContentFilterPlace = ContentFilterPlace.ANY,
) {
    private val prepared =
        rules
            .filter { it.enabled && it.appliesIn(place) && contentFilterValidationError(it) == null }
            .map { it to FilterCondition(it) }

    fun thread(
        board: BoardAddress,
        subject: String?,
        originalPost: String,
        threadId: Long? = null,
    ): ContentFilterRule? = threadMatch(board, subject, originalPost, threadId)?.rule

    fun comment(
        board: BoardAddress,
        text: String,
        threadId: Long? = null,
    ): ContentFilterRule? = commentMatch(board, text, threadId)?.rule

    fun threadMatch(
        board: BoardAddress,
        subject: String?,
        originalPost: String,
        threadId: Long? = null,
    ): FilterMatch? {
        for ((rule, condition) in prepared) {
            if (!rule.threads || !rule.appliesTo(board) || !rule.matchesThreadId(threadId)) continue
            val match = condition.builderThreadMatch(rule, subject.orEmpty(), originalPost)
            if (match != null) return match
        }
        return null
    }

    fun commentMatch(
        board: BoardAddress,
        text: String,
        threadId: Long? = null,
    ): FilterMatch? {
        for ((rule, condition) in prepared) {
            if (!rule.comments || !rule.appliesTo(board) || !rule.matchesThreadId(threadId)) continue
            condition.ranges(text)?.let { return FilterMatch(rule, FilterMatchField.COMMENT, it) }
        }
        return null
    }
}
