package com.solthariel.pawchan.domain.filter

import java.util.regex.Pattern

/** Prepared once; each term is quoted, so repeats and lookarounds cannot enter through user input. */
internal class LiteralFilterCondition(
    private val rule: ContentFilterRule,
) {
    private val flags = Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    private val groups = rule.conditionGroups().map { group -> group.map { Pattern.compile(Pattern.quote(it), flags) } }
    private val exceptions =
        rule.options.exceptions
            .flatMap(::filterAlternatives)
            .map { Pattern.compile(Pattern.quote(it), flags) }

    fun ranges(source: String): List<IntRange>? {
        if (groups.isEmpty() || exceptions.any { firstRange(it, source) != null }) return null
        val found = groups.map { group -> group.firstNotNullOfOrNull { firstRange(it, source) } }
        return when {
            rule.mode == ContentFilterMode.ALL_TERMS && found.any { it == null } -> null
            else -> found.filterNotNull().takeIf { it.isNotEmpty() }
        }
    }

    private fun firstRange(
        pattern: Pattern,
        source: String,
    ): IntRange? {
        val matcher = pattern.matcher(source)
        while (matcher.find()) {
            if (matcher.end() > matcher.start() && boundaries(source, matcher.start(), matcher.end())) {
                return matcher.start() until matcher.end()
            }
        }
        return null
    }

    private fun boundaries(
        source: String,
        start: Int,
        end: Int,
    ): Boolean {
        val boundary = rule.options.textMatch.boundary
        val left = start == 0 || !filterWordCodePoint(source.codePointBefore(start))
        val right = end == source.length || !filterWordCodePoint(source.codePointAt(end))
        return boundary == FilterBoundary.PART || (left && right)
    }
}

private fun filterWordCodePoint(point: Int): Boolean =
    Character.isLetterOrDigit(point) || point == '_'.code ||
        Character.getType(point) in
        setOf(
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.ENCLOSING_MARK.toInt(),
            Character.LETTER_NUMBER.toInt(),
            Character.OTHER_NUMBER.toInt(),
        )
