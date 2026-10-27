package com.solthariel.pawchan.domain.filter

internal class FilterCondition(
    rule: ContentFilterRule,
) {
    private val literal = LiteralFilterCondition(rule)

    fun ranges(source: String): List<IntRange>? = literal.ranges(source)
}
