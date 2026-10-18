package com.solthariel.pawchan.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
internal abstract class ThreadLibraryBackupDao {
    @Query("SELECT * FROM thread_library ORDER BY site, board, threadId")
    abstract suspend fun loadAll(): List<ThreadLibraryEntity>

    @Query("UPDATE thread_library SET favorite = 0")
    protected abstract suspend fun clearFavorites()

    @Query("UPDATE thread_library SET lastVisitedAtEpochMillis = 0")
    protected abstract suspend fun clearHistory()

    @Query("DELETE FROM thread_library WHERE favorite = 0 AND lastVisitedAtEpochMillis = 0")
    protected abstract suspend fun deleteUnused()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun write(entries: List<ThreadLibraryEntity>)

    @Transaction
    open suspend fun replace(
        entries: List<ThreadLibraryEntity>,
        favorites: Boolean,
        history: Boolean,
    ) {
        val before = loadAll().associateBy { Triple(it.site, it.board, it.threadId) }
        if (favorites) clearFavorites()
        if (history) clearHistory()
        write(
            entries.map { next ->
                val old = before[Triple(next.site, next.board, next.threadId)]
                val visitedAt = if (history) next.lastVisitedAtEpochMillis else old?.lastVisitedAtEpochMillis ?: 0L
                next.copy(
                    favorite = if (favorites) next.favorite else old?.favorite ?: false,
                    lastVisitedAtEpochMillis = visitedAt,
                )
            },
        )
        deleteUnused()
    }
}
