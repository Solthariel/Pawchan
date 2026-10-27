package com.solthariel.pawchan.domain.filter

/** Exceptions and ALL_TERMS see the whole post, not independent subject/body fragments. */
internal fun FilterCondition.builderThreadMatch(
    rule: ContentFilterRule,
    subject: String,
    body: String,
): FilterMatch? {
    val joined = "$subject\n$body"
    val matches = ranges(joined) ?: return null
    val bodyStart = subject.length + 1
    val bodyRanges =
        matches.mapNotNull { range ->
            if (range.last >= bodyStart) {
                (range.first.coerceAtLeast(bodyStart) - bodyStart)..(range.last - bodyStart)
            } else {
                null
            }
        }
    return if (bodyRanges.isNotEmpty()) {
        FilterMatch(rule, FilterMatchField.ORIGINAL_POST, bodyRanges)
    } else {
        val subjectRanges =
            matches.mapNotNull { range ->
                if (range.first < subject.length) range.first..range.last.coerceAtMost(subject.lastIndex) else null
            }
        FilterMatch(rule, FilterMatchField.SUBJECT, subjectRanges)
    }
}
