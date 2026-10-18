package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadId

internal fun Preferences.exportOwnMarkers(): Map<String, String> =
    asMap()
        .filterKeys { isOwnKey(it.name) }
        .map { (key, value) ->
            key.name to (value as Set<*>).map { requireNotNull(it as? String) }.sorted().joinToString(",")
        }.toMap()

internal fun validateOwnMarkers(values: Map<String, String>) {
    var totalIds = 0
    values.forEach { (key, value) ->
        require(isOwnKey(key))
        if (value.isNotEmpty()) {
            totalIds += value.count { it == ',' } + 1
            require(totalIds <= MAX_OWN_MARKERS)
            value.split(',').forEach { require((it.toLongOrNull() ?: 0L) > 0L) }
        }
    }
}

internal fun MutablePreferences.replaceOwnMarkers(values: Map<String, String>) {
    validateOwnMarkers(values)
    asMap().keys.filter { isOwnKey(it.name) }.forEach { remove(it) }
    values.forEach { (key, value) ->
        this[stringSetPreferencesKey(key)] = if (value.isEmpty()) emptySet() else value.split(',').toSet()
    }
}

private fun isOwnKey(key: String): Boolean {
    val parts = key.split(':')
    return parts.size == OWN_KEY_PARTS && parts[0] == "post" && parts[OWN_FIELD_PART] == "own" &&
        SiteId.parse(parts[1]) != null && BoardId.parse(parts[2]) != null &&
        parts[THREAD_PART].toLongOrNull()?.let(ThreadId::parse) != null
}

private const val MAX_OWN_MARKERS = 100_000
private const val OWN_KEY_PARTS = 5
private const val OWN_FIELD_PART = 4
private const val THREAD_PART = 3
