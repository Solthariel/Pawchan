package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress

enum class ContentFilterMode {
    ANY_PHRASE,
    ALL_WORDS,
    ORDERED,
    REGEX,
    EXACT_PHRASE,
}

enum class ContentFilterFields {
    SUBJECT,
    ORIGINAL_POST,
    EITHER,
}

enum class FilterMatchField {
    SUBJECT,
    ORIGINAL_POST,
    COMMENT,
}

data class ContentFilterOptions(
    val customLabel: String? = null,
    val wholeWords: Boolean = true,
    val maxGap: Int? = null,
    val exceptions: List<String> = emptyList(),
    val expiresAtMillis: Long? = null,
)

data class ContentFilterTargets(
    val threads: Boolean = true,
    val comments: Boolean = false,
    val fields: ContentFilterFields = ContentFilterFields.EITHER,
    val catalog: Boolean = true,
    val thread: Boolean = true,
    val threadIds: Set<Long> = emptySet(),
)

data class ContentFilterConditionSpec(
    val mode: ContentFilterMode = ContentFilterMode.EXACT_PHRASE,
    val values: List<String> = emptyList(),
)

/** Text condition with optional containing-thread scope; never an author or mirror filter. */
data class ContentFilterRule(
    val id: String,
    val site: String,
    val boards: Set<String> = emptySet(),
    val enabled: Boolean = true,
    val targets: ContentFilterTargets = ContentFilterTargets(),
    val condition: ContentFilterConditionSpec = ContentFilterConditionSpec(),
    val options: ContentFilterOptions = ContentFilterOptions(),
) {
    val threads: Boolean get() = targets.threads
    val comments: Boolean get() = targets.comments
    val fields: ContentFilterFields get() = targets.fields
    val mode: ContentFilterMode get() = condition.mode
    val values: List<String> get() = condition.values
    val label: String get() =
        options.customLabel?.takeIf { it.isNotBlank() } ?: values.joinToString(
            when (mode) {
                ContentFilterMode.ORDERED -> " → "
                ContentFilterMode.ALL_WORDS -> ", "
                else -> ", "
            },
        )

    fun appliesTo(
        board: BoardAddress,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean =
        enabled && site == board.site.value && (boards.isEmpty() || board.board.value in boards) &&
            (options.expiresAtMillis == null || nowMillis < options.expiresAtMillis)
}

/** Ranges refer to the original parsed text, not HTML or the normalized copy. */
data class FilterMatch(
    val rule: ContentFilterRule,
    val field: FilterMatchField,
    val ranges: List<IntRange>,
)
