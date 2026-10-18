package com.solthariel.pawchan.data.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "thread_watch",
    primaryKeys = ["site", "board", "threadId"],
    indices = [Index(value = ["enabledAtEpochMillis"])],
)
internal data class ThreadWatchEntity(
    val site: String,
    val board: String,
    val threadId: Long,
    val subject: String?,
    val lastKnownPostId: Long?,
    val enabledAtEpochMillis: Long,
)
