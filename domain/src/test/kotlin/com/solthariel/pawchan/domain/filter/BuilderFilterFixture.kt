package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId

internal val builderBoard = BoardAddress(SiteId.parse("dvach")!!, BoardId.parse("b")!!)

internal fun builderRule(
    mode: ContentFilterMode = ContentFilterMode.PHRASE,
    terms: List<String> = listOf("кот"),
    boundary: FilterBoundary = FilterBoundary.WORD,
    sensitive: Boolean = false,
    exceptions: List<String> = emptyList(),
): ContentFilterRule =
    ContentFilterRule(
        "test",
        "dvach",
        condition = ContentFilterConditionSpec(mode, terms),
        options = ContentFilterOptions(exceptions = exceptions, textMatch = FilterTextMatch(boundary, sensitive)),
    )
