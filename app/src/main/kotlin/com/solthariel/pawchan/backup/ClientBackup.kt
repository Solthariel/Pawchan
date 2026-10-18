package com.solthariel.pawchan.backup

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.workspace.WorkspaceState

internal data class ClientBackup(
    val settings: Map<String, String>,
    val favorites: List<ThreadLibraryEntry>,
    val bookmarks: WorkspaceState,
    val sections: Set<BackupSection> = DEFAULT_BACKUP_SECTIONS,
    val ownMarkers: Map<String, String> = emptyMap(),
    val metadata: BackupMetadata = BackupMetadata(),
)

internal data class BackupMetadata(
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String = "unknown",
)

internal const val CLIENT_BACKUP_MAX_BYTES = 32 * 1024 * 1024
internal const val BACKUP_MAX_UNPACKED_BYTES = 64 * 1024 * 1024
internal const val BACKUP_MAX_RECORDS = 100_000
