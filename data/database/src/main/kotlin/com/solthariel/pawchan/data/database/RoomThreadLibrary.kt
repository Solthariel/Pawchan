package com.solthariel.pawchan.data.database

import android.database.sqlite.SQLiteException
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress

class RoomThreadLibrary internal constructor(
    private val dao: ThreadLibraryDao,
    private val backupDao: ThreadLibraryBackupDao? = null,
    private val gate: MaintenanceGate? = null,
) : ThreadLibraryRepository,
    com.solthariel.pawchan.domain.library.HistoryCleanup {
    private suspend fun <T> guarded(block: suspend () -> T): DomainOutcome<T> =
        storageCall { if (gate == null) block() else gate.access(block) }

    override suspend fun loadBackupEntries(): DomainOutcome<List<ThreadLibraryEntry>> =
        guarded { requireNotNull(backupDao).loadAll().map { requireNotNull(it.toDomain()) } }

    override suspend fun replaceBackupEntries(
        entries: List<ThreadLibraryEntry>,
        favorites: Boolean,
        history: Boolean,
    ): DomainOutcome<Unit> =
        guarded {
            requireNotNull(backupDao).replace(
                entries.map { it.address.toEntity(it.subject, it.lastVisitedAtEpochMillis, it.favorite) },
                favorites,
                history,
            )
        }

    override suspend fun clearVisitHistory(): DomainOutcome<Unit> = guarded { dao.clearVisitHistory() }

    override suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ): DomainOutcome<Unit> =
        guarded {
            dao.recordVisit(address.toEntity(subject, visitedAtEpochMillis, favorite = false))
        }

    override suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ): DomainOutcome<Unit> =
        guarded {
            dao.setFavorite(address.toEntity(subject, updatedAtEpochMillis, favorite))
        }

    override suspend fun loadEntry(address: ThreadAddress): DomainOutcome<ThreadLibraryEntry?> =
        guarded {
            dao
                .loadEntry(
                    site = address.site.value,
                    board = address.board.board.value,
                    threadId = address.thread.value,
                )?.toDomain()
        }

    override suspend fun loadFavorites(): DomainOutcome<List<ThreadLibraryEntry>> =
        guarded { dao.loadFavorites().mapNotNull(ThreadLibraryEntity::toDomain) }

    override suspend fun loadEntries(): DomainOutcome<List<ThreadLibraryEntry>> =
        guarded {
            dao.loadEntries(LIBRARY_LIMIT).mapNotNull(ThreadLibraryEntity::toDomain)
        }
}

internal suspend fun <T> storageCall(block: suspend () -> T): DomainOutcome<T> =
    try {
        DomainOutcome.Success(block())
    } catch (_: SQLiteException) {
        DomainOutcome.Failure(DomainFailure.StorageUnavailable)
    } catch (_: java.io.IOException) {
        DomainOutcome.Failure(DomainFailure.StorageUnavailable)
    }

private fun ThreadAddress.toEntity(
    subject: String?,
    timestamp: Long,
    favorite: Boolean,
): ThreadLibraryEntity =
    ThreadLibraryEntity(
        site = site.value,
        board = board.board.value,
        threadId = thread.value,
        subject = subject,
        lastVisitedAtEpochMillis = timestamp,
        favorite = favorite,
    )

private fun ThreadLibraryEntity.toDomain(): ThreadLibraryEntry? {
    val address = storedThreadAddress(site, board, threadId)
    return if (address != null) {
        ThreadLibraryEntry(
            address = address,
            subject = subject,
            lastVisitedAtEpochMillis = lastVisitedAtEpochMillis,
            favorite = favorite,
        )
    } else {
        null
    }
}

private const val LIBRARY_LIMIT = 500
