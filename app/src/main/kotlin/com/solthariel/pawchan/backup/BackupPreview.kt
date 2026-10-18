package com.solthariel.pawchan.backup

import java.text.DateFormat
import java.util.Date

/** Counts and display metadata are prepared off Main with the decoded payload. */
internal data class BackupPreview(
    val backup: ClientBackup,
    val counts: Map<BackupSection, Int>,
    val description: String,
)

internal fun prepareBackupPreview(backup: ClientBackup): BackupPreview =
    BackupPreview(
        backup,
        backupCounts(backup),
        "Создана: ${DateFormat.getDateTimeInstance().format(Date(backup.metadata.createdAt))}\n" +
            "Версия приложения: ${backup.metadata.appVersion}",
    )
