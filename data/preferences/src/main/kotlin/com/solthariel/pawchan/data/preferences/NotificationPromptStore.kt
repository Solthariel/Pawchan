package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import java.io.IOException

/** Device-local first-run state. Deliberately excluded from portable settings and backups. */
class NotificationPromptStore(
    private val store: DataStore<Preferences>,
) {
    suspend fun claimFirstRequest(): Boolean =
        try {
            var claimed = false
            store.edit { values ->
                if (values[REQUESTED] != true) {
                    values[REQUESTED] = true
                    claimed = true
                }
            }
            claimed
        } catch (_: IOException) {
            false
        }
}

private val REQUESTED = booleanPreferencesKey("notifications_permission_requested")
