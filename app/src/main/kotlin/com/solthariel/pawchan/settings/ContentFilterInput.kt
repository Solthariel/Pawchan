package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule

internal fun filterInputValues(
    mode: String,
    text: String,
): List<String> =
    if (mode == ContentFilterMode.PHRASE.name) {
        listOf(text)
    } else {
        text.lines().map(String::trim).filter(String::isNotEmpty)
    }

internal fun filterModeLabel(mode: ContentFilterMode): String =
    when (mode) {
        ContentFilterMode.PHRASE -> "Фраза"
        ContentFilterMode.ANY_TERM -> "Любое из слов"
        ContentFilterMode.ALL_TERMS -> "Все слова"
        else -> "Неподдерживаемый формат"
    }

internal fun ContentFilterDraft.selectMode(next: ContentFilterMode) {
    mode = next.name
}

internal fun filterInputText(rule: ContentFilterRule): String = rule.values.joinToString("\n")
