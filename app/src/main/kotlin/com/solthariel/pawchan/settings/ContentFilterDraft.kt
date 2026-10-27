package com.solthariel.pawchan.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.filter.FilterBoundary
import com.solthariel.pawchan.domain.filter.FilterTextMatch
import com.solthariel.pawchan.domain.model.BoardId

internal class ContentFilterDraft(
    rule: ContentFilterRule,
) {
    var enabled by mutableStateOf(rule.enabled)
    var mode by mutableStateOf(rule.mode.name)
    var text by mutableStateOf(filterInputText(rule))
    var boards by mutableStateOf(rule.boards.joinToString(", "))
    var threadIds by mutableStateOf(rule.targets.threadIds.joinToString(", "))
    var boundary by mutableStateOf(
        rule.options.textMatch.boundary
            .takeUnless { it == FilterBoundary.PREFIX }
            ?.name ?: FilterBoundary.WORD.name,
    )
    var exceptions by mutableStateOf(rule.options.exceptions.joinToString("\n"))

    fun rule(base: ContentFilterRule): ContentFilterRule =
        base.copy(
            enabled = enabled,
            boards = boardValues().mapNotNull { BoardId.parse(it)?.value }.toSet(),
            targets = ContentFilterTargets(threadIds = threadIdParts().mapNotNull(String::toLongOrNull).toSet()),
            condition = ContentFilterConditionSpec(ContentFilterMode.valueOf(mode), filterInputValues(mode, text)),
            options =
                ContentFilterOptions(
                    exceptions = exceptions.lines().map(String::trim).filter(String::isNotEmpty),
                    textMatch = FilterTextMatch(FilterBoundary.valueOf(boundary)),
                ),
        )

    fun inputError(): String? =
        when {
            threadIdParts().any { it.toLongOrNull()?.let { id -> id > 0 } != true } -> {
                "Номера тредов должны быть положительными числами"
            }

            boardValues().any { BoardId.parse(it) == null } -> {
                "Некорректная доска"
            }

            else -> {
                null
            }
        }

    private fun threadIdParts(): List<String> = threadIds.split(',', ' ', '\n').filter(String::isNotBlank)

    private fun boardValues(): Set<String> =
        boards
            .split(',', ' ', '\n')
            .map { it.trim().trim('/') }
            .filter(String::isNotBlank)
            .toSet()

    companion object {
        val Saver =
            listSaver<ContentFilterDraft, Any>(
                save = {
                    listOf(
                        "builder-2",
                        it.enabled,
                        it.mode,
                        it.text,
                        it.boards,
                        it.threadIds,
                        it.boundary,
                        it.exceptions,
                    )
                },
                restore = { values ->
                    if (values.size != DRAFT_STATE_FIELDS || values.firstOrNull() != "builder-2") {
                        null
                    } else {
                        val fields = values.drop(1).iterator()
                        ContentFilterDraft(ContentFilterRule("draft", "draft")).apply {
                            enabled = fields.next() as Boolean
                            mode = fields.next() as String
                            text = fields.next() as String
                            boards = fields.next() as String
                            threadIds = fields.next() as String
                            boundary = fields.next() as String
                            exceptions = fields.next() as String
                        }
                    }
                },
            )
    }
}

private const val DRAFT_STATE_FIELDS = 8
