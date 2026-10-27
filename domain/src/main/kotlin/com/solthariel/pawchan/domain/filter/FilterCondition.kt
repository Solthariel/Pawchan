package com.solthariel.pawchan.domain.filter

internal class FilterCondition(
    private val rule: ContentFilterRule,
) {
    private val values = rule.values.map(::normalizedFilterText)
    private val exceptions =
        rule.options.exceptions
            .map(::normalizedFilterText)
            .filter { it.isNotEmpty() }
    private val regex =
        if (rule.mode == ContentFilterMode.REGEX) {
            BoundedFilterRegex(rule.values.singleOrNull().orEmpty())
        } else {
            null
        }

    fun ranges(source: String): List<IntRange>? {
        if (rule.values.isEmpty() || values.any { it.isEmpty() }) return null
        val text = FilterText(source)
        return when {
            exceptions.any { text.normalized.contains(it) } -> {
                null
            }

            regex != null -> {
                regex.find(source)?.let { listOf(it) }
            }

            else -> {
                normalizedRanges(text.normalized)?.map(text::original)
            }
        }
    }

    private fun normalizedRanges(text: String): List<IntRange>? =
        if (rule.mode == ContentFilterMode.EXACT_PHRASE) exactPhrase(text) else null

    private fun exactPhrase(text: String): List<IntRange>? {
        val value = values.singleOrNull() ?: return null
        var offset = text.indexOf(value)
        while (offset >= 0 && !exactBounds(text, offset, value)) {
            offset = text.indexOf(value, offset + 1)
        }
        return if (offset >= 0) listOf(offset until offset + value.length) else null
    }

    private fun exactBounds(
        text: String,
        start: Int,
        value: String,
    ): Boolean {
        val left = !wordCharacter(value.first()) || start == 0 || !wordCharacter(text[start - 1])
        val end = start + value.length
        return left && (!wordCharacter(value.last()) || end == text.length || !wordCharacter(text[end]))
    }
}
