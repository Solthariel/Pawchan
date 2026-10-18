package com.solthariel.pawchan.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ThreadLibraryEntity::class, ThreadWatchEntity::class],
    version = 2,
    exportSchema = true,
)
internal abstract class PawchanDatabase : RoomDatabase() {
    abstract fun threadLibraryBackupDao(): ThreadLibraryBackupDao

    abstract fun threadLibraryDao(): ThreadLibraryDao

    abstract fun threadWatchDao(): ThreadWatchDao
}
