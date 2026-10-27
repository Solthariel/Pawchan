package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.filter.ContentFilterRule
import java.util.Base64

/** Versioned UTF-8 records. Base64 keeps punctuation/tabs in phrases literal. */
internal fun encodeContentFilters(rules: List<ContentFilterRule>): String =
    rules
        .filter {
            com.solthariel.pawchan.domain.filter
                .contentFilterRecordValidationError(it) == null
        }.joinToString("\n") { rule ->
            listOf(
                "5",
                rule.id,
                rule.site,
                rule.boards.joinToString("\n"),
                rule.enabled.toString(),
                rule.threads.toString(),
                rule.comments.toString(),
                rule.fields.name,
                rule.mode.name,
                encodeItems(rule.values),
                rule.options.customLabel.orEmpty(),
                rule.options.wholeWords.toString(),
                rule.options.maxGap
                    ?.toString()
                    .orEmpty(),
                encodeItems(rule.options.exceptions),
                rule.options.expiresAtMillis
                    ?.toString()
                    .orEmpty(),
                rule.targets.catalog.toString(),
                rule.targets.thread.toString(),
                rule.targets.threadIds.joinToString(","),
                rule.options.textMatch.boundary.name,
                rule.options.textMatch.caseSensitive
                    .toString(),
            ).joinToString("\t") { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }
        }

internal fun decodeContentFilters(source: String): List<ContentFilterRule> =
    source
        .lineSequence()
        .filter { it.isNotBlank() }
        .mapNotNull(::decodeContentFilterRecord)
        .toList()

private fun encodeItems(items: List<String>): String =
    items.joinToString(";") {
        Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8))
    }
