package com.solthariel.pawchan.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.filter.ContentFilterFields
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.model.BoardId
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

internal class ContentFilterDraft(
    rule: ContentFilterRule,
) {
    var enabled by mutableStateOf(rule.enabled)
    var threads by mutableStateOf(rule.threads)
    var comments by mutableStateOf(rule.comments)
    var mode by mutableStateOf(rule.mode.name)
    var fields by mutableStateOf(rule.fields.name)
    var tokenizeWords by mutableStateOf(filterWordsAreSimple(rule.values))
    var text by mutableStateOf(filterInputText(rule))
    var threadIds by mutableStateOf(rule.targets.threadIds.joinToString(", "))
    var catalog by mutableStateOf(rule.targets.catalog)
    var thread by mutableStateOf(rule.targets.thread)
    var boards by mutableStateOf(rule.boards.joinToString(", "))
    var label by mutableStateOf(rule.options.customLabel.orEmpty())
    var wholeWords by mutableStateOf(rule.options.wholeWords)
    var gap by mutableStateOf(
        rule.options.maxGap
            ?.toString()
            .orEmpty(),
    )
    var exceptions by mutableStateOf(rule.options.exceptions.joinToString("\n"))
    var expires by mutableStateOf(filterExpirationDate(rule.options.expiresAtMillis))

    fun rule(base: ContentFilterRule): ContentFilterRule =
        base.copy(
            enabled = enabled,
            boards =
                boardValues()
                    .mapNotNull { BoardId.parse(it)?.value }
                    .toSet(),
            options =
                ContentFilterOptions(
                    if (label == base.options.customLabel.orEmpty()) {
                        base.options.customLabel
                    } else {
                        label.ifBlank { null }
                    },
                    wholeWords,
                    gap.toIntOrNull(),
                    if (exceptions == base.options.exceptions.joinToString("\n")) {
                        base.options.exceptions
                    } else {
                        lines(exceptions)
                    },
                    if (expires == filterExpirationDate(base.options.expiresAtMillis)) {
                        base.options.expiresAtMillis
                    } else {
                        expiration()
                    },
                ),
            targets =
                base.targets.copy(
                    threads = threads,
                    comments = comments,
                    fields = ContentFilterFields.valueOf(fields),
                    catalog = catalog,
                    thread = thread,
                    threadIds = parsedThreadIds(),
                ),
            condition =
                base.condition.copy(
                    mode = ContentFilterMode.valueOf(mode),
                    values =
                        if (
                            mode == base.mode.name && text == filterInputText(base) &&
                            tokenizeWords == filterWordsAreSimple(base.values)
                        ) {
                            base.values
                        } else {
                            filterInputValues(mode, text, tokenizeWords)
                        },
                ),
        )

    fun inputError(): String? =
        when {
            threadIdParts().any { it.toLongOrNull()?.let { id -> id > 0 } != true } -> {
                "Номера тредов должны быть положительными числами"
            }

            !threads && comments && !thread -> {
                "Для ответов включите применение в треде"
            }

            boardValues().any { BoardId.parse(it) == null } -> {
                "Некорректная доска"
            }

            mode == ContentFilterMode.ORDERED.name &&
                gap.isNotBlank() &&
                (gap.toIntOrNull() == null || gap.toInt() < 0) -> {
                "Некорректное расстояние"
            }

            expires.isNotBlank() && expiration() == null -> {
                "Дата: ГГГГ-ММ-ДД"
            }

            else -> {
                null
            }
        }

    private fun threadIdParts(): List<String> = threadIds.split(',', ' ', '\n').filter(String::isNotBlank)

    private fun parsedThreadIds(): Set<Long> = threadIdParts().mapNotNull(String::toLongOrNull).toSet()

    private fun boardValues(): Set<String> =
        boards
            .split(',', ' ', '\n')
            .map { it.trim().trim('/') }
            .filter { it.isNotBlank() }
            .toSet()

    private fun expiration(): Long? =
        try {
            expires
                .takeIf { it.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) }
                ?.let {
                    LocalDate
                        .parse(it)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()
                }
        } catch (_: DateTimeParseException) {
            null
        }

    companion object {
        val Saver =
            listSaver<ContentFilterDraft, Any>(
                save = {
                    listOf(
                        it.threads,
                        it.comments,
                        it.mode,
                        it.fields,
                        it.text,
                        it.boards,
                        it.label,
                        it.wholeWords,
                        it.gap,
                        it.exceptions,
                        it.expires,
                        it.tokenizeWords,
                        it.catalog,
                        it.thread,
                        it.threadIds,
                        it.enabled,
                    )
                },
                restore = { values ->
                    ContentFilterDraft(ContentFilterRule("draft", "draft")).apply {
                        threads = values[0] as Boolean
                        comments = values[1] as Boolean
                        mode = values[2] as String
                        fields = values[3] as String
                        text = values[4] as String
                        boards = values[5] as String
                        label = values[6] as String
                        wholeWords = values[7] as Boolean
                        gap = values[8] as String
                        exceptions = values[9] as String
                        expires = values[10] as String
                        tokenizeWords = values.getOrNull(11) as? Boolean ?: true
                        catalog = values.getOrNull(12) as? Boolean ?: true
                        thread = values.getOrNull(13) as? Boolean ?: true
                        threadIds = values.getOrNull(14) as? String ?: ""
                        enabled = values.getOrNull(15) as? Boolean ?: true
                    }
                },
            )
    }
}

private fun lines(text: String): List<String> =
    text
        .lines()
        .map(String::trim)
        .filter(String::isNotEmpty)
