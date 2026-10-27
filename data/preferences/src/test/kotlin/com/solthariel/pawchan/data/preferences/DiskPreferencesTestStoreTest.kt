package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DiskPreferencesTestStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun repeatedAtomicReplacementPersistsTheLatestValueAcrossScopeReopening() =
        runBlocking {
            val file = File(temporary.root, "replace.preferences_pb")
            val key = longPreferencesKey("counter")
            repeat(3) { reopen ->
                val job = SupervisorJob()
                try {
                    val store = diskPreferencesTestStore(file, CoroutineScope(job + Dispatchers.IO))
                    if (reopen > 0) assertEquals(reopen * 2L, store.data.first()[key])
                    repeat(2) { update ->
                        val expected = reopen * 2L + update + 1L
                        store.edit { it[key] = expected }
                        assertEquals(expected, store.data.first()[key])
                        assertTrue(file.isFile && file.length() > 0)
                        assertFalse(File(file.absolutePath + ".tmp").exists())
                    }
                } finally {
                    job.cancelAndJoin()
                }
            }
        }

    @Test fun concurrentStoresForTheSameFileAreStillRejected() =
        runBlocking {
            val file = File(temporary.root, "one-owner.preferences_pb")
            val firstJob = SupervisorJob()
            val secondJob = SupervisorJob()
            try {
                val first = diskPreferencesTestStore(file, CoroutineScope(firstJob + Dispatchers.IO))
                first.edit { it[longPreferencesKey("counter")] = 1L }
                val duplicate = diskPreferencesTestStore(file, CoroutineScope(secondJob + Dispatchers.IO))
                try {
                    duplicate.data.first()
                    fail("Two simultaneously active stores must not be allowed")
                } catch (error: IllegalStateException) {
                    assertTrue(error.message.orEmpty().contains("multiple DataStores active"))
                }
            } finally {
                secondJob.cancelAndJoin()
                firstJob.cancelAndJoin()
            }
        }
}
