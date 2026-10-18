package com.solthariel.pawchan.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
internal abstract class ThreadLibraryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insert(entry: ThreadLibraryEntity): Long

    @Query(
        """
        UPDATE thread_library
        SET subject = COALESCE(:subject, subject), lastVisitedAtEpochMillis = :visitedAt
        WHERE site = :site AND board = :board AND threadId = :threadId
        """,
    )
    protected abstract suspend fun updateVisit(
        site: String,
        board: String,
        threadId: Long,
        subject: String?,
        visitedAt: Long,
    )

    @Query(
        """
        UPDATE thread_library
        SET subject = COALESCE(:subject, subject), favorite = :favorite
        WHERE site = :site AND board = :board AND threadId = :threadId
        """,
    )
    protected abstract suspend fun updateFavorite(
        site: String,
        board: String,
        threadId: Long,
        subject: String?,
        favorite: Boolean,
    )

    @Query(
        """
        SELECT * FROM thread_library
        WHERE site = :site AND board = :board AND threadId = :threadId
        """,
    )
    abstract suspend fun loadEntry(
        site: String,
        board: String,
        threadId: Long,
    ): ThreadLibraryEntity?

    @Query(
        """
        SELECT * FROM thread_library
        ORDER BY favorite DESC, lastVisitedAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    abstract suspend fun loadEntries(limit: Int): List<ThreadLibraryEntity>

    @Query("SELECT * FROM thread_library WHERE favorite = 1 ORDER BY lastVisitedAtEpochMillis DESC")
    abstract suspend fun loadFavorites(): List<ThreadLibraryEntity>

    @Query("DELETE FROM thread_library WHERE favorite = 0")
    protected abstract suspend fun deleteNonFavorites()

    @Query("UPDATE thread_library SET lastVisitedAtEpochMillis = 0 WHERE favorite = 1")
    protected abstract suspend fun resetFavoriteVisits()

    @Transaction
    open suspend fun clearVisitHistory() {
        deleteNonFavorites()
        resetFavoriteVisits()
    }

    @Transaction
    open suspend fun recordVisit(entry: ThreadLibraryEntity) {
        insert(entry)
        updateVisit(
            site = entry.site,
            board = entry.board,
            threadId = entry.threadId,
            subject = entry.subject,
            visitedAt = entry.lastVisitedAtEpochMillis,
        )
    }

    @Transaction
    open suspend fun setFavorite(entry: ThreadLibraryEntity) {
        insert(entry)
        updateFavorite(
            site = entry.site,
            board = entry.board,
            threadId = entry.threadId,
            subject = entry.subject,
            favorite = entry.favorite,
        )
    }
}
