package com.solthariel.pawchan.backup

import com.solthariel.pawchan.data.preferences.PortableSettings
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import java.io.IOException

/** All operations must run inside the shared maintenance gate. */
internal class BackupStores(
    private val settings: PortableSettings,
    private val library: ThreadLibraryRepository,
    val workspace: WorkspaceRepository,
) {
    suspend fun snapshot(sections: Set<BackupSection>): ClientBackup {
        require(sections.isNotEmpty())
        val entries =
            if (sections.any { it == BackupSection.FAVORITES || it == BackupSection.HISTORY }) {
                (library.loadBackupEntries() as? DomainOutcome.Success)?.value
                    ?: throw IOException("Ошибка чтения библиотеки")
            } else {
                emptyList()
            }
        return selectedBackup(settings.export(), entries, workspace.load(), sections).copy(
            ownMarkers = if (BackupSection.OWN_POSTS in sections) settings.exportOwn() else emptyMap(),
            metadata = BackupMetadata(appVersion = com.solthariel.pawchan.BuildConfig.VERSION_NAME),
        )
    }

    fun validate(backup: ClientBackup) {
        settings.validate(backup.settings, backup.ownMarkers)
        val expected =
            settings
                .defaults()
                .keys
                .filter { preferenceSection(it) in backup.sections }
                .toSet()
        require(backup.settings.keys == expected) { "Неполный раздел настроек" }
    }

    suspend fun apply(backup: ClientBackup) {
        settings.replace(backup.settings, backup.ownMarkers.takeIf { BackupSection.OWN_POSTS in backup.sections })
        if (backup.sections.any { it == BackupSection.FAVORITES || it == BackupSection.HISTORY }) {
            val result =
                library.replaceBackupEntries(
                    backup.favorites,
                    BackupSection.FAVORITES in backup.sections,
                    BackupSection.HISTORY in backup.sections,
                )
            if (result is DomainOutcome.Failure) throw IOException("Ошибка записи библиотеки")
        }
        if (backup.sections.any { it == BackupSection.HIDDEN || it == BackupSection.OWN_POSTS }) {
            workspace.update { replaceWorkspace(it, backup) }
        }
    }
}
