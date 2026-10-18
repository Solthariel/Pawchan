package com.solthariel.pawchan.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface ThreadWatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ThreadWatchEntity)

    @Query(
        """
        DELETE FROM thread_watch
        WHERE site = :site AND board = :board AND threadId = :threadId
        """,
    )
    suspend fun delete(
        site: String,
        board: String,
        threadId: Long,
    )

    @Query(
        """
        UPDATE thread_watch
        SET lastKnownPostId = CASE
            WHEN lastKnownPostId IS NULL OR lastKnownPostId < :postId THEN :postId
            ELSE lastKnownPostId
        END
        WHERE site = :site AND board = :board AND threadId = :threadId
        """,
    )
    suspend fun updateCheckpoint(
        site: String,
        board: String,
        threadId: Long,
        postId: Long,
    )

    @Query(
        """
        SELECT * FROM thread_watch
        WHERE site = :site AND board = :board AND threadId = :threadId
        """,
    )
    suspend fun load(
        site: String,
        board: String,
        threadId: Long,
    ): ThreadWatchEntity?

    @Query("SELECT * FROM thread_watch ORDER BY enabledAtEpochMillis ASC")
    suspend fun loadAll(): List<ThreadWatchEntity>
}
