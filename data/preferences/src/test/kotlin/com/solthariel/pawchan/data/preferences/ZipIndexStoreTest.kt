package com.solthariel.pawchan.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

class ZipIndexStoreTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun writesAndReopensAfterClosingTheFirstScope() =
        runBlocking {
            val file = File(temporary.root, "index.preferences_pb")
            val firstJob = SupervisorJob()
            val secondJob = SupervisorJob()
            try {
                val first = ZipIndexStore(diskPreferencesTestStore(file, CoroutineScope(firstJob + Dispatchers.IO)))
                assertNull(first.read())
                first.update { "[{\"uri\":\"content://one\"}]" }
                firstJob.cancelAndJoin()
                val second = ZipIndexStore(diskPreferencesTestStore(file, CoroutineScope(secondJob + Dispatchers.IO)))
                assertEquals("[{\"uri\":\"content://one\"}]", second.read())
            } finally {
                firstJob.cancelAndJoin()
                secondJob.cancelAndJoin()
            }
        }

    @Test fun concurrentUpdatesDoNotLoseChanges() =
        runBlocking {
            val job = SupervisorJob()
            try {
                val file = File(temporary.root, "concurrent.preferences_pb")
                val store = ZipIndexStore(diskPreferencesTestStore(file, CoroutineScope(job + Dispatchers.IO)))
                (0 until 100)
                    .map { value ->
                        async(Dispatchers.IO) { store.update { previous -> "${previous.orEmpty()}$value," } }
                    }.awaitAll()
                assertEquals(
                    (0 until 100).map(Int::toString).toSet(),
                    store
                        .read()!!
                        .split(',')
                        .filter { it.isNotEmpty() }
                        .toSet(),
                )
                assertEquals(100, store.read()!!.split(',').count { it.isNotEmpty() })
            } finally {
                job.cancelAndJoin()
            }
        }

    @Test fun failedAndCancelledTransactionsLeaveThePreviousValue() =
        runBlocking {
            val job = SupervisorJob()
            try {
                val file = File(temporary.root, "failed.preferences_pb")
                val store = ZipIndexStore(diskPreferencesTestStore(file, CoroutineScope(job + Dispatchers.IO)))
                store.update { "before" }
                val cancellation = CancellationException("cancelled")
                try {
                    store.update { throw cancellation }
                    fail("Cancellation must propagate")
                } catch (error: CancellationException) {
                    assertEquals(cancellation.message, error.message)
                }
                val started = CompletableDeferred<Unit>()
                val blocked = CompletableDeferred<Unit>()
                val pending =
                    async(Dispatchers.IO) {
                        store.update {
                            started.complete(Unit)
                            blocked.await()
                            "after"
                        }
                    }
                started.await()
                pending.cancelAndJoin()
                assertEquals("before", store.read())
            } finally {
                job.cancelAndJoin()
            }
        }

    @Test fun diskWriteFailureDoesNotReplacePreviousValue() =
        runBlocking {
            val file = File(temporary.root, "io-failure.preferences_pb")
            val job = SupervisorJob()
            val failingFileSystem = FailingMoveFileSystem()
            try {
                val dataStore =
                    PreferenceDataStoreFactory.create(
                        storage =
                            OkioStorage(failingFileSystem, PreferencesSerializer) {
                                file.absoluteFile.toOkioPath()
                            },
                        scope = CoroutineScope(job + Dispatchers.IO),
                    )
                val store = ZipIndexStore(dataStore)
                store.update { "before" }
                failingFileSystem.failMoves = true
                try {
                    store.update { "after" }
                    fail("Disk write failure must propagate")
                } catch (error: IOException) {
                    assertEquals("injected move failure", error.message)
                }
                try {
                    store.read()
                    fail("Uncommitted cache must not be returned after a failed write")
                } catch (error: IOException) {
                    assertEquals("ZIP index unavailable after a failed write; restart required", error.message)
                }
                try {
                    store.update { "later" }
                    fail("Writes must not use an uncommitted cache")
                } catch (error: IOException) {
                    assertEquals("ZIP index unavailable after a failed write; restart required", error.message)
                }
            } finally {
                job.cancelAndJoin()
            }
            val reopenedJob = SupervisorJob()
            try {
                val reopened =
                    ZipIndexStore(diskPreferencesTestStore(file, CoroutineScope(reopenedJob + Dispatchers.IO)))
                assertEquals("before", reopened.read())
                reopened.update { "recovered" }
                assertEquals("recovered", reopened.read())
            } finally {
                reopenedJob.cancelAndJoin()
            }
        }

    @Test fun readAndWriteIoFailuresAreNotReportedAsSuccess() =
        runBlocking {
            val failure = IOException("storage unavailable")
            val store = ZipIndexStore(FailingDataStore(failure))
            try {
                store.read()
                fail("Read failure must propagate")
            } catch (error: IOException) {
                assertSame(failure, error)
            }
            try {
                store.update { "replacement" }
                fail("Write failure must propagate")
            } catch (error: IOException) {
                assertSame(failure, error)
            }
        }
}

private class FailingDataStore(
    private val failure: IOException,
) : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw failure }

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw failure
}

private class FailingMoveFileSystem : ForwardingFileSystem(FileSystem.SYSTEM) {
    @Volatile var failMoves = false

    override fun atomicMove(
        source: Path,
        target: Path,
    ) {
        if (failMoves) throw IOException("injected move failure")
        super.atomicMove(source, target)
    }
}
