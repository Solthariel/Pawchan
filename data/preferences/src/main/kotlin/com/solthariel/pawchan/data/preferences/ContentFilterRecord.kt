package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterFields
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import java.util.Base64

internal fun decodeContentFilterRecord(line: String): ContentFilterRule? =
    try {
        val fields = line.split('\t').map { String(Base64.getDecoder().decode(it), Charsets.UTF_8) }
        val supported =
            (fields.size == LEGACY_FILTER_FIELD_COUNT && fields[FIELD_VERSION] == "1") ||
                (fields.size == VERSION_TWO_FILTER_FIELD_COUNT && fields[FIELD_VERSION] == "2") ||
                (fields.size == VERSION_THREE_FILTER_FIELD_COUNT && fields[FIELD_VERSION] == "3") ||
                (fields.size == CURRENT_FILTER_FIELD_COUNT && fields[FIELD_VERSION] == "4")
        if (supported) fields.toContentFilterRule() else null
    } catch (_: IllegalArgumentException) {
        null // Ignore only an invalid record, not the rest of the settings.
    }

private fun List<String>.toContentFilterRule(): ContentFilterRule =
    ContentFilterRule(
        id = this[FIELD_ID],
        site = this[FIELD_SITE],
        boards = this[FIELD_BOARDS].lines().filter { it.isNotBlank() }.toSet(),
        enabled = this[FIELD_ENABLED].toBooleanStrict(),
        options = filterOptions(),
        targets =
            ContentFilterTargets(
                threads = this[FIELD_THREADS].toBooleanStrict(),
                comments = this[FIELD_COMMENTS].toBooleanStrict(),
                fields = ContentFilterFields.valueOf(this[FIELD_TARGET_FIELDS]),
                threadIds = filterThreadIds(),
                catalog = if (this[FIELD_VERSION] in setOf("3", "4")) this[FIELD_CATALOG].toBooleanStrict() else true,
                thread = if (this[FIELD_VERSION] in setOf("3", "4")) this[FIELD_THREAD].toBooleanStrict() else true,
            ),
        condition =
            ContentFilterConditionSpec(
                mode = ContentFilterMode.valueOf(this[FIELD_MODE]),
                values = filterValues(),
            ),
    )

private fun List<String>.filterOptions(): ContentFilterOptions =
    if (this[FIELD_VERSION] == "1") {
        ContentFilterOptions()
    } else {
        ContentFilterOptions(
            customLabel = this[FIELD_LABEL].ifBlank { null },
            wholeWords = this[FIELD_WHOLE_WORDS].toBooleanStrict(),
            maxGap = this[FIELD_MAX_GAP].takeIf { it.isNotEmpty() }?.toInt(),
            exceptions = decodeItems(this[FIELD_EXCEPTIONS]),
            expiresAtMillis = this[FIELD_EXPIRES_AT].takeIf { it.isNotEmpty() }?.toLong(),
        )
    }

private fun List<String>.filterValues(): List<String> =
    if (this[FIELD_VERSION] == "1") {
        this[FIELD_VALUES].lines().filter { it.isNotBlank() }
    } else {
        decodeItems(this[FIELD_VALUES])
    }

private fun decodeItems(source: String): List<String> =
    if (source.isEmpty()) {
        emptyList()
    } else {
        source.split(';').map {
            String(Base64.getDecoder().decode(it), Charsets.UTF_8)
        }
    }

private fun List<String>.filterThreadIds(): Set<Long> =
    if (this[FIELD_VERSION] == "4" && this[FIELD_THREAD_IDS].isNotEmpty()) {
        this[FIELD_THREAD_IDS].split(',').map { it.toLong().also { id -> require(id > 0) } }.toSet()
    } else {
        emptySet()
    }

private const val LEGACY_FILTER_FIELD_COUNT = 10
private const val VERSION_TWO_FILTER_FIELD_COUNT = 15
private const val VERSION_THREE_FILTER_FIELD_COUNT = 17
private const val CURRENT_FILTER_FIELD_COUNT = 18
private const val FIELD_VERSION = 0
private const val FIELD_ID = 1
private const val FIELD_SITE = 2
private const val FIELD_BOARDS = 3
private const val FIELD_ENABLED = 4
private const val FIELD_THREADS = 5
private const val FIELD_COMMENTS = 6
private const val FIELD_TARGET_FIELDS = 7
private const val FIELD_MODE = 8
private const val FIELD_VALUES = 9
private const val FIELD_LABEL = 10
private const val FIELD_WHOLE_WORDS = 11
private const val FIELD_MAX_GAP = 12
private const val FIELD_EXCEPTIONS = 13
private const val FIELD_EXPIRES_AT = 14
private const val FIELD_CATALOG = 15
private const val FIELD_THREAD = 16
private const val FIELD_THREAD_IDS = 17
