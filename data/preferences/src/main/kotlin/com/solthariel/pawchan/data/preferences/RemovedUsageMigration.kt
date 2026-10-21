package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

/** Remove both legacy aggregates and retained identities, without resetting other statistics. */
internal object RemovedUsageMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData.asMap().keys.any(::oldUsageKey)

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            asMap().keys.filter(::oldUsageKey).forEach { remove(it) }
        }

    override suspend fun cleanUp() = Unit
}

private fun oldUsageKey(key: Preferences.Key<*>): Boolean {
    val parts = key.name.split('|')
    return parts.size == REMOVED_USAGE_KEY_PARTS &&
        parts[0] in setOf("usage", "usage_seen", "usage_unique") &&
        parts[2] == "POSTS_VIEWED"
}

private const val REMOVED_USAGE_KEY_PARTS = 3
