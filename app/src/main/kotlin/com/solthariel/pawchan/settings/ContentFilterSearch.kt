package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterRule

internal fun ContentFilterRule.matchesRuleSearch(query: String): Boolean {
    val matchesQuery =
        query.isBlank() || listOf(label, site, boards.joinToString()).any { it.contains(query, ignoreCase = true) }
    return matchesQuery
}
