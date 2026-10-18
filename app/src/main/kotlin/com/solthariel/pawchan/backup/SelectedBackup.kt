package com.solthariel.pawchan.backup

import com.solthariel.pawchan.data.drafts.WorkspaceTransfer
import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.workspace.WorkspaceState

internal fun selectedBackup(
    settings: Map<String, String>,
    entries: List<ThreadLibraryEntry>,
    state: WorkspaceState,
    sections: Set<BackupSection>,
): ClientBackup =
    ClientBackup(
        settings.filterKeys { key ->
            when (key) {
                "app_content_filters_v1" -> BackupSection.FILTERS in sections
                "favorite_boards_v2" -> BackupSection.FAVORITES in sections
                else -> BackupSection.SETTINGS in sections
            }
        },
        entries
            .filter {
                (it.favorite && BackupSection.FAVORITES in sections) ||
                    (it.lastVisitedAtEpochMillis > 0 && BackupSection.HISTORY in sections)
            }.map {
                it.copy(
                    favorite = it.favorite && BackupSection.FAVORITES in sections,
                    lastVisitedAtEpochMillis =
                        if (BackupSection.HISTORY in sections) it.lastVisitedAtEpochMillis else 0L,
                )
            },
        WorkspaceTransfer.select(
            state,
            BackupSection.HIDDEN in sections,
            BackupSection.OWN_POSTS in sections,
        ),
        sections,
    )
