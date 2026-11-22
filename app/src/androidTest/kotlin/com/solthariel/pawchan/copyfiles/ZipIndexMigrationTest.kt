package com.solthariel.pawchan.copyfiles

import android.content.Context
import androidx.core.content.edit
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.solthariel.pawchan.data.preferences.ZipIndexStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ZipIndexMigrationTest {
    @Test fun copiesLegacyOnceAndDoesNotReplaceNewDataOnReopen() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "zip-migration-${UUID.randomUUID()}"
            val legacy = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val firstJob = SupervisorJob()
            val secondJob = SupervisorJob()
            try {
                val original = "[{\"route\":\"thread/b/1\",\"uri\":\"content://legacy\"}]"
                legacy.edit(commit = true) { putString("documents", original) }
                val first = ZipIndexStore.create(context, name, CoroutineScope(firstJob + Dispatchers.IO))
                assertEquals(original, first.read())
                assertEquals(original, legacy.getString("documents", null))
                first.update { "new" }
                firstJob.cancelAndJoin()
                val second = ZipIndexStore.create(context, name, CoroutineScope(secondJob + Dispatchers.IO))
                assertEquals("new", second.read())
                assertEquals(original, legacy.getString("documents", null))
            } finally {
                firstJob.cancelAndJoin()
                secondJob.cancelAndJoin()
                context.deleteSharedPreferences(name)
                context.preferencesDataStoreFile(name).delete()
            }
        }

    @Test fun existingDataWinsEvenWithoutMigrationMarker() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "zip-preexisting-${UUID.randomUUID()}"
            val legacy = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val seedJob = SupervisorJob()
            val testJob = SupervisorJob()
            try {
                legacy.edit(commit = true) { putString("documents", "old") }
                val seed =
                    PreferenceDataStoreFactory.create(scope = CoroutineScope(seedJob + Dispatchers.IO)) {
                        context.preferencesDataStoreFile(name)
                    }
                seed.edit { it[stringPreferencesKey("documents")] = "new" }
                seedJob.cancelAndJoin()
                val store = ZipIndexStore.create(context, name, CoroutineScope(testJob + Dispatchers.IO))
                assertEquals("new", store.read())
                assertEquals("old", legacy.getString("documents", null))
            } finally {
                seedJob.cancelAndJoin()
                testJob.cancelAndJoin()
                context.deleteSharedPreferences(name)
                context.preferencesDataStoreFile(name).delete()
            }
        }

    @Test fun failedMigrationCanRetryWithoutLosingLegacy() =
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val name = "zip-retry-${UUID.randomUUID()}"
            val legacy = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val job = SupervisorJob()
            try {
                legacy.edit(commit = true) { putInt("documents", 7) }
                val store = ZipIndexStore.create(context, name, CoroutineScope(job + Dispatchers.IO))
                try {
                    store.read()
                    fail("Invalid legacy type must not be treated as an empty index")
                } catch (_: ClassCastException) {
                    assertEquals(7, legacy.getInt("documents", 0))
                }
                legacy.edit(commit = true) { putString("documents", "recovered") }
                assertEquals("recovered", store.read())
                assertEquals("recovered", legacy.getString("documents", null))
            } finally {
                job.cancelAndJoin()
                context.deleteSharedPreferences(name)
                context.preferencesDataStoreFile(name).delete()
            }
        }
}
