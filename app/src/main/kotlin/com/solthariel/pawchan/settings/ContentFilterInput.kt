package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.model.BoardAddress
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

internal fun newContentFilterRule(
    board: BoardAddress,
    text: String = "",
    comment: Boolean = false,
): ContentFilterRule =
    ContentFilterRule(
        id = UUID.randomUUID().toString(),
        site = board.site.value,
        boards = setOf(board.board.value),
        targets = ContentFilterTargets(threads = !comment, comments = comment),
        condition = ContentFilterConditionSpec(ContentFilterMode.EXACT_PHRASE, listOf(text)),
    )

internal fun filterInputValues(
    mode: String,
    text: String,
    tokenizeWords: Boolean,
): List<String> =
    when {
        mode == ContentFilterMode.EXACT_PHRASE.name || mode == ContentFilterMode.REGEX.name -> {
            listOf(text)
        }

        mode == ContentFilterMode.ALL_WORDS.name && tokenizeWords -> {
            text.split(FILTER_WORD_SEPARATOR).filter(String::isNotBlank)
        }

        else -> {
            text.lines().map(String::trim).filter(String::isNotEmpty)
        }
    }

internal fun filterModeLabel(mode: ContentFilterMode): String =
    when (mode) {
        ContentFilterMode.EXACT_PHRASE -> "Точная фраза"
        ContentFilterMode.ALL_WORDS -> "Все слова"
        ContentFilterMode.ANY_PHRASE -> "Любая фраза (старый режим)"
        ContentFilterMode.ORDERED -> "Фрагменты по порядку"
        ContentFilterMode.REGEX -> "Регулярное выражение"
    }

internal fun ContentFilterDraft.selectMode(next: ContentFilterMode) {
    mode = next.name
    if (next == ContentFilterMode.ALL_WORDS) {
        tokenizeWords = true
        wholeWords = true
    }
}

internal fun filterExpirationDate(value: Long?): String =
    value
        ?.let {
            Instant
                .ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        }.orEmpty()

private val FILTER_WORD_SEPARATOR = Regex("[,\\s\\p{Z}]+")

internal fun filterWordsAreSimple(values: List<String>): Boolean =
    values.none { value ->
        value.any { char -> char.isWhitespace() || char == ',' }
    }

internal fun filterInputText(rule: ContentFilterRule): String =
    rule.values.joinToString(
        if (rule.mode == ContentFilterMode.ALL_WORDS && filterWordsAreSimple(rule.values)) " " else "\n",
    )
