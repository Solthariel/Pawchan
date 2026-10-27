package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.coroutines.CoroutineScope
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

/** Real protobuf files and atomic replacement, independent of mock Android SDK_INT on desktop JVMs. */
internal fun diskPreferencesTestStore(
    file: File,
    scope: CoroutineScope,
    migrations: List<DataMigration<Preferences>> = emptyList(),
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { file.absoluteFile.toOkioPath() },
        migrations = migrations,
        scope = scope,
    )
