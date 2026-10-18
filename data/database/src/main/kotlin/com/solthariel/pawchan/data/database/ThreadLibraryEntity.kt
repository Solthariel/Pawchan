package com.solthariel.pawchan.data.database

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "thread_library",
    primaryKeys = ["site", "board", "threadId"],
    indices = [Index(value = ["favorite", "lastVisitedAtEpochMillis"])],
)
internal data class ThreadLibraryEntity(
    val site: String,
    val board: String,
    val threadId: Long,
    val subject: String?,
    val lastVisitedAtEpochMillis: Long,
    val favorite: Boolean,
)
