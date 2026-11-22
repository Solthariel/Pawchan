package com.solthariel.pawchan.downloads

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

internal enum class DownloadStatus { QUEUED, RUNNING, WAITING, SUCCEEDED, FAILED, CANCELLED }

internal data class DownloadRecord(
    val id: String,
    val url: String,
    val destination: String,
    val name: String,
    val origin: DownloadOrigin,
    val created: Long,
    val status: DownloadStatus,
)

internal data class DownloadOrigin(
    val threadRoute: String?,
    val archiveId: Long?,
    val postId: Long? = null,
)

internal sealed interface DownloadHistoryState {
    data object Loading : DownloadHistoryState

    data object Error : DownloadHistoryState

    data class Content(
        val records: List<DownloadRecord>,
        val refreshFailures: Long = 0,
    ) : DownloadHistoryState
}

/** One application-owned database. File bytes stay in the user's document provider, never in this database. */
internal class DownloadHistoryStore(
    context: Context,
    name: String = "media-download-history.db",
) : SQLiteOpenHelper(context, name, null, 2) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val revision = MutableStateFlow(0L)
    private var lastContent: DownloadHistoryState.Content? = null
    val cachedRecords =
        revision
            .map {
                try {
                    DownloadHistoryState.Content(readableDatabase.readDownloadRecords()).also { lastContent = it }
                } catch (_: SQLiteException) {
                    downloadHistoryReadFailure(lastContent).also {
                        if (it is DownloadHistoryState.Content) lastContent = it
                    }
                }
            }.stateIn(scope, SharingStarted.Eagerly, DownloadHistoryState.Loading)

    fun refresh() {
        revision.update { it + 1 }
    }

    override fun close() {
        scope.cancel()
        super.close()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE downloads (id TEXT PRIMARY KEY, url TEXT NOT NULL, destination TEXT NOT NULL, " +
                "name TEXT NOT NULL, thread_route TEXT, archive_id INTEGER, post_id INTEGER, " +
                "created INTEGER NOT NULL, status TEXT NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX active_destination ON downloads(destination) " +
                "WHERE status IN ('QUEUED', 'RUNNING', 'WAITING')",
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE downloads ADD COLUMN post_id INTEGER")
    }

    fun insert(record: DownloadRecord) {
        val values =
            ContentValues().apply {
                put("id", record.id)
                put("url", record.url)
                put("destination", record.destination)
                put("name", record.name)
                put("thread_route", record.origin.threadRoute)
                if (record.origin.archiveId == null) {
                    putNull("archive_id")
                } else {
                    put("archive_id", record.origin.archiveId)
                }
                if (record.origin.postId == null) putNull("post_id") else put("post_id", record.origin.postId)
                put("created", record.created)
                put("status", record.status.name)
            }
        writableDatabase.insertOrThrow("downloads", null, values)
        revision.update { it + 1 }
    }

    fun setDestination(
        id: String,
        destination: String,
    ) {
        val values = ContentValues().apply { put("destination", destination) }
        if (writableDatabase.update("downloads", values, "id = ?", arrayOf(id)) > 0) revision.update { it + 1 }
    }

    fun setStatus(
        id: String,
        status: DownloadStatus,
    ) {
        val values = ContentValues().apply { put("status", status.name) }
        if (writableDatabase.update("downloads", values, "id = ? AND status != ?", arrayOf(id, status.name)) > 0) {
            revision.update { it + 1 }
        }
    }

    fun setStatusIfUnchanged(
        id: String,
        old: DownloadStatus,
        new: DownloadStatus,
    ) {
        val values = ContentValues().apply { put("status", new.name) }
        if (writableDatabase.update("downloads", values, "id = ? AND status = ?", arrayOf(id, old.name)) > 0) {
            revision.update { it + 1 }
        }
    }

    /** Never replace a completed worker's result with a late stop observation. */
    fun setStatusIfActive(
        id: String,
        status: DownloadStatus,
    ): Boolean {
        val values = ContentValues().apply { put("status", status.name) }
        val where = "id = ? AND status != ? AND status IN (?, ?, ?)"
        val args =
            arrayOf(
                id,
                status.name,
                DownloadStatus.QUEUED.name,
                DownloadStatus.RUNNING.name,
                DownloadStatus.WAITING.name,
            )
        val changed = writableDatabase.update("downloads", values, where, args) > 0
        if (changed) revision.update { it + 1 }
        return changed
    }

    fun remove(id: String) {
        if (writableDatabase.delete("downloads", "id = ?", arrayOf(id)) > 0) revision.update { it + 1 }
    }

    fun clearFinished() {
        val where = "status IN (?, ?, ?)"
        val args = arrayOf(DownloadStatus.SUCCEEDED.name, DownloadStatus.FAILED.name, DownloadStatus.CANCELLED.name)
        if (writableDatabase.delete("downloads", where, args) > 0) revision.update { it + 1 }
    }
}

private fun SQLiteDatabase.readDownloadRecords(): List<DownloadRecord> =
    query("downloads", null, null, null, null, null, "created DESC, rowid DESC").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val url = cursor.getString(cursor.getColumnIndexOrThrow("url"))
                val destination = cursor.getString(cursor.getColumnIndexOrThrow("destination"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val route = cursor.getString(cursor.getColumnIndexOrThrow("thread_route"))
                val archiveColumn = cursor.getColumnIndexOrThrow("archive_id")
                val archiveId = if (cursor.isNull(archiveColumn)) null else cursor.getLong(archiveColumn)
                val postColumn = cursor.getColumnIndexOrThrow("post_id")
                val postId = if (cursor.isNull(postColumn)) null else cursor.getLong(postColumn)
                val created = cursor.getLong(cursor.getColumnIndexOrThrow("created"))
                val status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
                val parsed = DownloadStatus.entries.firstOrNull { it.name == status } ?: DownloadStatus.FAILED
                add(
                    DownloadRecord(
                        id,
                        url,
                        destination,
                        name,
                        DownloadOrigin(route, archiveId, postId),
                        created,
                        parsed,
                    ),
                )
            }
        }
    }

internal fun downloadHistoryReadFailure(previous: DownloadHistoryState.Content?): DownloadHistoryState =
    if (previous != null && previous.records.isNotEmpty()) {
        previous.copy(refreshFailures = previous.refreshFailures + 1)
    } else {
        DownloadHistoryState.Error
    }
