package com.solthariel.pawchan.data.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

/** The caller owns the scope and must create only one store per file at a time. */
class ZipIndexStore internal constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val mutex = Mutex()
    private var failedWrite: IOException? = null

    suspend fun read(): String? =
        mutex.withLock {
            checkWriteState()
            dataStore.data.first()[DOCUMENTS]
        }

    suspend fun update(transform: suspend (String?) -> String) =
        mutex.withLock {
            checkWriteState()
            try {
                dataStore.edit { preferences ->
                    preferences[DOCUMENTS] = transform(preferences[DOCUMENTS])
                }
            } catch (error: IOException) {
                // DataStore can update its in-memory cache before an atomic file replacement fails.
                // Do not use that uncommitted cache for later edits; a fresh process reloads the file.
                failedWrite = error
                throw error
            }
            Unit
        }

    private fun checkWriteState() {
        failedWrite?.let { throw IOException("ZIP index unavailable after a failed write; restart required", it) }
    }

    companion object {
        fun create(
            context: Context,
            name: String,
            scope: CoroutineScope,
        ): ZipIndexStore {
            val app = context.applicationContext
            val store =
                PreferenceDataStoreFactory.create(
                    scope = scope,
                    migrations = listOf(LegacyZipIndexMigration(app, name)),
                ) { app.preferencesDataStoreFile(name) }
            return ZipIndexStore(store)
        }
    }
}

/** Copy once, without clearing legacy: an interrupted migration can safely run again. */
private class LegacyZipIndexMigration(
    private val context: Context,
    private val name: String,
) : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[MIGRATED] != true

    override suspend fun migrate(currentData: Preferences): Preferences =
        currentData.toMutablePreferences().apply {
            if (this[DOCUMENTS] == null) {
                val legacy = context.getSharedPreferences(name, Context.MODE_PRIVATE).getString(DOCUMENTS.name, null)
                if (legacy != null) this[DOCUMENTS] = legacy
            }
            this[MIGRATED] = true
        }

    override suspend fun cleanUp() = Unit
}

private val DOCUMENTS = stringPreferencesKey("documents")
private val MIGRATED = booleanPreferencesKey("legacy_documents_migrated")
