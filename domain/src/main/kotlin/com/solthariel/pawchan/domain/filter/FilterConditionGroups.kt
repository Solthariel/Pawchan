package com.solthariel.pawchan.domain.filter

/** Rows are AND groups in ALL_TERMS; commas separate literal alternatives, never regex. */
fun filterAlternatives(value: String): List<String> =
    value
        .split(',', '\n', '\r')
        .map(String::trim)
        .filter(String::isNotEmpty)
        .distinct()

internal fun ContentFilterRule.conditionGroups(): List<List<String>> =
    if (mode == ContentFilterMode.PHRASE) {
        values.map(::listOf)
    } else {
        values.flatMap { value -> value.lines().map(::filterAlternatives) }
    }
