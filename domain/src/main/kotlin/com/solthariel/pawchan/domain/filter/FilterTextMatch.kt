package com.solthariel.pawchan.domain.filter

/** Literal builder rules never interpret user input as regex. */
enum class FilterBoundary { WORD, PREFIX, PART }

data class FilterTextMatch(
    val boundary: FilterBoundary = FilterBoundary.WORD,
    val caseSensitive: Boolean = false,
)

val ContentFilterMode.isBuilderMode: Boolean
    get() =
        this == ContentFilterMode.PHRASE || this == ContentFilterMode.ANY_TERM ||
            this == ContentFilterMode.ALL_TERMS
