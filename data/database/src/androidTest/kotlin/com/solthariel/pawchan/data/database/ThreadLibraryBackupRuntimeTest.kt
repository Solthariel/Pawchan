package com.solthariel.pawchan.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThreadLibraryBackupRuntimeTest {
    @Test fun favoritesReplacementPreservesHistoryAndRemovesOldFavorites() =
        runBlocking {
            val db = database()
            try {
                val backup = db.threadLibraryBackupDao()
                db.threadLibraryDao().recordVisit(entity(42L, true, 100L))
                backup.replace(listOf(entity(43L, true, 0L)), favorites = true, history = false)
                val entries = backup.loadAll().associateBy { it.threadId }
                assertEquals(false, entries.getValue(42L).favorite)
                assertEquals(100L, entries.getValue(42L).lastVisitedAtEpochMillis)
                assertEquals(true, entries.getValue(43L).favorite)
                backup.replace(emptyList(), favorites = true, history = false)
                assertEquals(listOf(42L), backup.loadAll().map { it.threadId })
            } finally {
                db.close()
            }
        }

    @Test fun historyReplacementPreservesFavoritesAndReplacesDates() =
        runBlocking {
            val db = database()
            try {
                val backup = db.threadLibraryBackupDao()
                db.threadLibraryDao().recordVisit(entity(42L, true, 500L))
                backup.replace(listOf(entity(42L, false, 100L)), favorites = false, history = true)
                val entry = backup.loadAll().single()
                assertTrue(entry.favorite)
                assertEquals(100L, entry.lastVisitedAtEpochMillis)
                backup.replace(emptyList(), favorites = false, history = true)
                assertTrue(backup.loadAll().single().favorite)
                assertEquals(0L, backup.loadAll().single().lastVisitedAtEpochMillis)
            } finally {
                db.close()
            }
        }

    @Test fun fullExportIsNotLimitedByPresentationQuery() =
        runBlocking {
            val db = database()
            try {
                val backup = db.threadLibraryBackupDao()
                backup.replace((1L..750L).map { entity(it, false, it) }, favorites = true, history = true)
                assertEquals(500, db.threadLibraryDao().loadEntries(500).size)
                assertEquals(750, backup.loadAll().size)
            } finally {
                db.close()
            }
        }

    private fun database(): PawchanDatabase =
        Room
            .inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().targetContext,
                PawchanDatabase::class.java,
            ).build()

    private fun entity(
        id: Long,
        favorite: Boolean,
        visited: Long,
    ) = ThreadLibraryEntity(
        site = "dvach",
        board = "b",
        threadId = id,
        subject = "Тред $id",
        favorite = favorite,
        lastVisitedAtEpochMillis = visited,
    )
}
