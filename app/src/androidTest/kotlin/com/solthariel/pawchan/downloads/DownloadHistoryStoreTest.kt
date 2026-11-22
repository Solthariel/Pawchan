package com.solthariel.pawchan.downloads

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DownloadHistoryStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "test-downloads-${UUID.randomUUID()}.db"
    private lateinit var store: DownloadHistoryStore
    private val one =
        DownloadRecord(
            id = "1",
            url = "https://example.test/a",
            destination = "content://file/1",
            name = "a",
            origin = DownloadOrigin(null, null),
            created = 1,
            status = DownloadStatus.QUEUED,
        )

    @Before fun setup() {
        store = DownloadHistoryStore(context, name)
    }

    @After fun cleanup() {
        store.close()
        context.deleteDatabase(name)
    }

    @Test fun clearHistoryKeepsActiveAndNeverDeletesFiles() =
        runBlocking {
            store.insert(one)
            store.insert(one.copy(id = "2", destination = "content://file/2", status = DownloadStatus.SUCCEEDED))
            store.clearFinished()
            assertEquals(listOf(one), awaitRecords(store) { it == listOf(one) })
        }

    @Test fun cachedHistoryPublishesChangesAndReplaysThem() =
        runBlocking {
            store.insert(one)
            val first =
                withTimeout(5_000) {
                    store.cachedRecords.filterIsInstance<DownloadHistoryState.Content>().first { one in it.records }
                }
            assertEquals(listOf(one), first.records)
            assertEquals(first, store.cachedRecords.value)
        }

    @Test fun migrationPreservesOldRecordsAndStoresPostId() =
        runBlocking {
            val oldName = "migration-${UUID.randomUUID()}.db"
            context.openOrCreateDatabase(oldName, Context.MODE_PRIVATE, null).use { db ->
                db.execSQL(
                    "CREATE TABLE downloads (id TEXT PRIMARY KEY, url TEXT NOT NULL, destination TEXT NOT NULL, " +
                        "name TEXT NOT NULL, thread_route TEXT, archive_id INTEGER, created INTEGER NOT NULL, " +
                        "status TEXT NOT NULL)",
                )
                db.execSQL(
                    "INSERT INTO downloads VALUES ('old', 'https://example.test/old', 'content://file/old', " +
                        "'old', 'thread/dvach/b/12', NULL, 1, 'SUCCEEDED')",
                )
                db.version = 1
            }
            val migrated = DownloadHistoryStore(context, oldName)
            try {
                migrated.insert(one.copy(origin = DownloadOrigin("thread/dvach/b/12", null, 44)))
                val entries = awaitRecords(migrated) { it.size == 2 }.associateBy { it.id }
                assertEquals(null, entries.getValue("old").origin.postId)
                assertEquals(44L, entries.getValue("1").origin.postId)
            } finally {
                migrated.close()
                context.deleteDatabase(oldName)
            }
        }

    private suspend fun awaitRecords(
        history: DownloadHistoryStore,
        predicate: (List<DownloadRecord>) -> Boolean,
    ): List<DownloadRecord> =
        withTimeout(5_000) {
            history.cachedRecords
                .filterIsInstance<DownloadHistoryState.Content>()
                .first { predicate(it.records) }
                .records
        }

    @Test fun preventsTwoActiveWritesToSameDestination() {
        store.insert(one)
        assertThrows(SQLiteConstraintException::class.java) { store.insert(one.copy(id = "2")) }
        store.setStatus("1", DownloadStatus.FAILED)
        store.insert(one.copy(id = "2"))
    }

    @Test fun lateStopCannotReplaceCompletedStatus() =
        runBlocking {
            store.insert(one)
            store.setStatus("1", DownloadStatus.SUCCEEDED)
            assertEquals(false, store.setStatusIfActive("1", DownloadStatus.CANCELLED))
            val records = awaitRecords(store) { it.singleOrNull()?.status == DownloadStatus.SUCCEEDED }
            assertEquals(DownloadStatus.SUCCEEDED, records.single().status)
        }

    @Test fun compareAndSetCannotOverwriteCompletedWorker() =
        runBlocking {
            store.insert(one)
            store.setStatus("1", DownloadStatus.SUCCEEDED)
            store.setStatusIfUnchanged("1", DownloadStatus.QUEUED, DownloadStatus.FAILED)
            assertEquals(
                DownloadStatus.SUCCEEDED,
                awaitRecords(store) { entries -> entries.singleOrNull()?.status == DownloadStatus.SUCCEEDED }
                    .single()
                    .status,
            )
        }
}
