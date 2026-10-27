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
        ContentFilterMode.ANY_TERM -> "Любое слово"
        ContentFilterMode.ALL_TERMS -> "Все слова"
        else -> "Неподдерживаемый формат"
    }

internal fun ContentFilterDraft.selectMode(next: ContentFilterMode) {
    mode = next.name
}

internal fun filterInputText(rule: ContentFilterRule): String = rule.values.joinToString("\n")

internal fun filterModeHint(mode: String): String =
    when (mode) {
        ContentFilterMode.ANY_TERM.name -> {
            "Достаточно одного условия. Каждое слово или фраза — с новой строки."
        }

        ContentFilterMode.ALL_TERMS.name -> {
            "Нужны все условия, порядок не важен. Каждое слово или фраза — с новой строки."
        }

        else -> {
            "Искать введённую фразу подряд. Пробелы и переносы имеют значение."
        }
    }

internal fun filterBoundaryHint(boundary: String): String =
    if (boundary == "PART") {
        "«кот» найдётся в «котик», «скот» и «котлета». Регистр не важен."
    } else {
        "«кот» найдётся отдельно, но не в «котик» и «скот». Регистр не важен."
    }
