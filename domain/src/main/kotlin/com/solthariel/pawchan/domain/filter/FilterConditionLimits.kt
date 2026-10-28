package com.solthariel.pawchan.domain.filter

/** Bound user-controlled rules, never truncate the post being checked. */
internal fun oversizedFilterCondition(rule: ContentFilterRule): Boolean =
    rule.values.sumOf { it.length.toLong() } + rule.options.exceptions.sumOf { it.length.toLong() } >
        MAX_FILTER_CONDITION_CHARACTERS ||
        rule.values.size > MAX_FILTER_VALUES || rule.options.exceptions.size > MAX_FILTER_VALUES ||
        rule.conditionGroups().sumOf { it.size } > MAX_FILTER_VALUES ||
        rule.options.exceptions.sumOf { filterAlternatives(it).size } > MAX_FILTER_VALUES

internal const val MAX_FILTER_VALUES = 128
internal const val MAX_FILTER_CONDITION_CHARACTERS = 32_768
