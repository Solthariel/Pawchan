package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NotificationPromptStoreTest {
    @Test fun onlyOneConcurrentFirstRunRequestIsClaimed() =
        runBlocking {
            val store = NotificationPromptStore(MemoryStore())
            val results = List(8) { async { store.claimFirstRequest() } }.awaitAll()
            assertEquals(1, results.count { it })
            assertFalse(store.claimFirstRequest())
        }

    @Test fun recreationAndPortableBackupsDoNotResetOrExportTheFlag() =
        runBlocking {
            val data = MemoryStore()
            assertTrue(NotificationPromptStore(data).claimFirstRequest())
            assertFalse(NotificationPromptStore(data).claimFirstRequest())
            assertFalse(exportPortablePreferences(data.value).containsKey("notifications_permission_requested"))
        }

    @Test fun storageFailureDoesNotCrashStartupOrConsumeTheNextSuccessfulRequest() =
        runBlocking {
            val data = MemoryStore()
            val store = NotificationPromptStore(data)
            data.fail = true
            assertFalse(store.claimFirstRequest())
            data.fail = false
            assertTrue(store.claimFirstRequest())
        }

    private class MemoryStore : DataStore<Preferences> {
        private val mutex = Mutex()
        private val state = MutableStateFlow(emptyPreferences())
        val value get() = state.value
        var fail = false
        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            mutex.withLock {
                if (fail) throw IOException("unavailable")
                transform(state.value).also { state.value = it }
            }
    }
}
