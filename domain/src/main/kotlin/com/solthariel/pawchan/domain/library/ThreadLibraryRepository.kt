package com.solthariel.pawchan.domain.library

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress

data class ThreadLibraryEntry(
    val address: ThreadAddress,
    val subject: String?,
    val lastVisitedAtEpochMillis: Long,
    val favorite: Boolean,
)

interface ThreadLibraryRepository {
    suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ): DomainOutcome<Unit>

    suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ): DomainOutcome<Unit>

    suspend fun loadEntry(address: ThreadAddress): DomainOutcome<ThreadLibraryEntry?>

    suspend fun loadEntries(): DomainOutcome<List<ThreadLibraryEntry>>

    /** Full portable snapshot; never use the presentation-limited loadEntries for backups. */
    suspend fun loadBackupEntries(): DomainOutcome<List<ThreadLibraryEntry>> =
        DomainOutcome.Failure(com.solthariel.pawchan.domain.model.DomainFailure.StorageUnavailable)

    suspend fun replaceBackupEntries(
        entries: List<ThreadLibraryEntry>,
        favorites: Boolean,
        history: Boolean,
    ): DomainOutcome<Unit> = DomainOutcome.Failure(com.solthariel.pawchan.domain.model.DomainFailure.StorageUnavailable)

    /** Favorites are not subject to the recent-history presentation limit. */
    suspend fun loadFavorites(): DomainOutcome<List<ThreadLibraryEntry>> =
        when (val result = loadEntries()) {
            is DomainOutcome.Success -> DomainOutcome.Success(result.value.filter { it.favorite })
            is DomainOutcome.Failure -> result
        }
}
