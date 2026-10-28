package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal val CONTENT_FILTER_RECORDS = stringPreferencesKey("app_content_filters_v1")
internal val CONTENT_FILTER_RESET_NOTICE = booleanPreferencesKey("app_content_filter_reset_notice")

/** Discard unsupported records, never reinterpret their commas under the new semantics. */
internal object ContentFilterResetMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[CONTENT_FILTER_RECORDS]
            .orEmpty()
            .lineSequence()
            .any { it.isNotBlank() && decodeContentFilterRecord(it) == null }

    override suspend fun migrate(currentData: Preferences): Preferences = discardUnsupportedFilterRecords(currentData)

    override suspend fun cleanUp() = Unit
}

internal fun discardUnsupportedFilterRecords(currentData: Preferences): Preferences {
    val raw = currentData[CONTENT_FILTER_RECORDS].orEmpty()
    return if (raw.lineSequence().any { it.isNotBlank() && decodeContentFilterRecord(it) == null }) {
        currentData.toMutablePreferences().apply {
            this[CONTENT_FILTER_RECORDS] = encodeContentFilters(decodeContentFilters(raw))
            this[CONTENT_FILTER_RESET_NOTICE] = true
        }
    } else {
        currentData
    }
}
