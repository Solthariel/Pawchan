package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

/** One-time reset requested for this release; later manual choices are never overwritten. */
internal object PostAppearanceMigration : DataMigration<Preferences> {
    private val completed = booleanPreferencesKey("post_appearance_reset_0110")
    private val bubbles = booleanPreferencesKey("app_bubble_posts")

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[completed] != true

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            this[bubbles] = false
            this[completed] = true
        }

    override suspend fun cleanUp() = Unit
}
