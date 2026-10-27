package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.filter.FilterBoundary
import com.solthariel.pawchan.domain.filter.FilterTextMatch
import com.solthariel.pawchan.domain.filter.contentFilterRecordValidationError
import java.util.Base64

/** Version five is a clean break: old records are discarded, not converted or kept dormant. */
internal fun decodeContentFilterRecord(line: String): ContentFilterRule? =
    try {
        val fields = line.split('\t').map { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }
        if (fields.size != BUILDER_RECORD_FIELDS || fields[FIELD_VERSION] != "5") null else fields.builderRule()
    } catch (_: IllegalArgumentException) {
        null
    }

private fun List<String>.builderRule(): ContentFilterRule? {
    val targets =
        ContentFilterTargets(
            threads = this[FIELD_THREADS].toBooleanStrict(),
            comments = this[FIELD_COMMENTS].toBooleanStrict(),
            fields =
                com.solthariel.pawchan.domain.filter.ContentFilterFields
                    .valueOf(this[FIELD_FIELDS]),
            catalog = this[FIELD_CATALOG].toBooleanStrict(),
            thread = this[FIELD_THREAD].toBooleanStrict(),
            threadIds =
                this[FIELD_THREAD_IDS]
                    .split(',')
                    .filter(String::isNotEmpty)
                    .map { it.toLong().also { id -> require(id > 0) } }
                    .toSet(),
        )
    val rule =
        ContentFilterRule(
            id = this[FIELD_ID],
            site = this[FIELD_SITE],
            boards = this[FIELD_BOARDS].lines().filter(String::isNotBlank).toSet(),
            enabled = this[FIELD_ENABLED].toBooleanStrict(),
            targets = targets,
            condition =
                ContentFilterConditionSpec(
                    ContentFilterMode.valueOf(this[FIELD_MODE]),
                    decodeItems(this[FIELD_VALUES]),
                ),
            options =
                ContentFilterOptions(
                    customLabel = this[FIELD_LABEL].ifBlank { null },
                    exceptions = decodeItems(this[FIELD_EXCEPTIONS]),
                    textMatch =
                        FilterTextMatch(
                            FilterBoundary.valueOf(this[FIELD_BOUNDARY]),
                            this[FIELD_CASE_SENSITIVE].toBooleanStrict(),
                        ),
                ),
        )
    return rule.takeIf { contentFilterRecordValidationError(it) == null }
}

private fun decodeItems(source: String): List<String> =
    if (source.isEmpty()) {
        emptyList()
    } else {
        source.split(';').map {
            String(Base64.getDecoder().decode(it), Charsets.UTF_8)
        }
    }

private const val BUILDER_RECORD_FIELDS = 20
private const val FIELD_VERSION = 0
private const val FIELD_ID = 1
private const val FIELD_SITE = 2
private const val FIELD_BOARDS = 3
private const val FIELD_ENABLED = 4
private const val FIELD_THREADS = 5
private const val FIELD_COMMENTS = 6
private const val FIELD_FIELDS = 7
private const val FIELD_MODE = 8
private const val FIELD_VALUES = 9
private const val FIELD_LABEL = 10
private const val FIELD_EXCEPTIONS = 13
private const val FIELD_CATALOG = 15
private const val FIELD_THREAD = 16
private const val FIELD_THREAD_IDS = 17
private const val FIELD_BOUNDARY = 18
private const val FIELD_CASE_SENSITIVE = 19
