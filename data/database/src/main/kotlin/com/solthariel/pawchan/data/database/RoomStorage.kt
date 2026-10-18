package com.solthariel.pawchan.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import java.io.File

class RoomStorage private constructor(
    private val database: PawchanDatabase,
    gate: MaintenanceGate,
) : AutoCloseable {
    val library = RoomThreadLibrary(database.threadLibraryDao(), database.threadLibraryBackupDao(), gate)
    val watches = RoomThreadWatchRepository(database.threadWatchDao())

    override fun close() {
        database.close()
    }

    companion object {
        fun create(context: Context): RoomStorage {
            val database =
                Room
                    .databaseBuilder(
                        context.applicationContext,
                        PawchanDatabase::class.java,
                        DATABASE_NAME,
                    ).addMigrations(MIGRATION_1_2)
                    .build()
            return RoomStorage(database, MaintenanceGate(File(context.filesDir, "backup-maintenance")))
        }
    }
}

internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS thread_watch (
                    site TEXT NOT NULL,
                    board TEXT NOT NULL,
                    threadId INTEGER NOT NULL,
                    subject TEXT,
                    lastKnownPostId INTEGER,
                    enabledAtEpochMillis INTEGER NOT NULL,
                    PRIMARY KEY(site, board, threadId)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS index_thread_watch_enabledAtEpochMillis
                ON thread_watch(enabledAtEpochMillis)
                """.trimIndent(),
            )
        }
    }

private const val DATABASE_NAME = "pawchan.db"
