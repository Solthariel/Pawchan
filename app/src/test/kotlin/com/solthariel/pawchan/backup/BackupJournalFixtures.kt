package com.solthariel.pawchan.backup

import com.solthariel.pawchan.data.preferences.PortableSettings
import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import java.io.IOException

internal class BackupTestSettings : PortableSettings {
    var values = mapOf("theme" to "SYSTEM", "favorite_boards_v2" to "", "app_content_filters_v1" to "")
    var own = emptyMap<String, String>()

    override fun defaults() = mapOf("theme" to "SYSTEM", "favorite_boards_v2" to "", "app_content_filters_v1" to "")

    override suspend fun export() = values

    override suspend fun exportOwn() = own

    override fun validate(
        values: Map<String, String>,
        own: Map<String, String>,
    ) {
        require(values.keys.all { it in defaults() })
    }

    override suspend fun replace(
        values: Map<String, String>,
        own: Map<String, String>?,
    ) {
        this.values = this.values + values
        if (own != null) this.own = own
    }
}

internal class BackupTestLibrary : ThreadLibraryRepository {
    var entries = emptyList<ThreadLibraryEntry>()
    var failures = 0
    var onReplace: suspend () -> Unit = {}

    override suspend fun loadBackupEntries() = DomainOutcome.Success(entries)

    override suspend fun loadEntries() = DomainOutcome.Success(entries.take(500))

    override suspend fun loadEntry(address: ThreadAddress): DomainOutcome<ThreadLibraryEntry?> {
        val entry = entries.firstOrNull { it.address == address }
        return DomainOutcome.Success(entry)
    }

    override suspend fun recordVisit(
        address: ThreadAddress,
        subject: String?,
        visitedAtEpochMillis: Long,
    ) = DomainOutcome.Success(Unit)

    override suspend fun setFavorite(
        address: ThreadAddress,
        subject: String?,
        favorite: Boolean,
        updatedAtEpochMillis: Long,
    ) = DomainOutcome.Success(Unit)

    override suspend fun replaceBackupEntries(
        entries: List<ThreadLibraryEntry>,
        favorites: Boolean,
        history: Boolean,
    ): DomainOutcome<Unit> {
        if (failures > 0) {
            failures--
            return DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        }
        onReplace()
        this.entries = entries
        return DomainOutcome.Success(Unit)
    }
}

internal class BackupTestWorkspace : WorkspaceRepository {
    var state = WorkspaceState()
    var failAfterWrite = false

    override suspend fun load() = state

    override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState {
        state = transform(state)
        if (failAfterWrite) {
            failAfterWrite = false
            throw IOException("simulated disk failure")
        }
        return state
    }
}
