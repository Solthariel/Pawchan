package com.solthariel.pawchan.backup

import android.content.ContentResolver
import android.net.Uri
import com.solthariel.pawchan.data.maintenance.BackupProtection
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal class ClientBackupService(
    private val resolver: ContentResolver,
    private val gate: MaintenanceGate,
    private val stores: BackupStores,
) {
    suspend fun prepare(
        sections: Set<BackupSection>,
        password: CharArray?,
    ): ByteArray =
        withContext(Dispatchers.IO) {
            val snapshot = gate.access { stores.snapshot(sections) }
            stores.validate(snapshot)
            val archive = encodeClientBackup(snapshot)
            try {
                BackupProtection.seal(archive, password)
            } catch (failure: java.security.GeneralSecurityException) {
                throw IOException("Шифрование недоступно", failure)
            } finally {
                archive.fill(0)
            }
        }

    suspend fun write(
        uri: Uri,
        bytes: ByteArray,
    ) = withContext(Dispatchers.IO) {
        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Нет доступа к файлу")
        stream.use { output -> output.write(bytes) }
    }

    suspend fun read(uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {
            resolver.openInputStream(uri)?.use { readClientBackup(it) } ?: throw IOException("Нет доступа к файлу")
        }

    suspend fun inspect(
        bytes: ByteArray,
        password: CharArray?,
    ): BackupPreview =
        withContext(Dispatchers.IO) {
            val archive = BackupProtection.open(bytes, password)
            try {
                prepareBackupPreview(decodeClientBackup(archive).also(stores::validate))
            } catch (failure: NoSuchElementException) {
                throw IllegalArgumentException("Повреждённая или несовместимая копия", failure)
            } catch (failure: IllegalStateException) {
                throw IllegalArgumentException("Повреждённая структура копии", failure)
            } finally {
                archive.fill(0)
            }
        }

    suspend fun restore(
        backup: ClientBackup,
        sections: Set<BackupSection>,
    ) = withContext(Dispatchers.IO) {
        require(sections.isNotEmpty() && backup.sections.containsAll(sections))
        val selected =
            selectedBackup(backup.settings, backup.favorites, backup.bookmarks, sections).copy(
                ownMarkers = backup.ownMarkers.takeIf { BackupSection.OWN_POSTS in sections }.orEmpty(),
                metadata = backup.metadata,
            )
        BackupUndoJournal(gate, stores).restore(selected)
    }
}
