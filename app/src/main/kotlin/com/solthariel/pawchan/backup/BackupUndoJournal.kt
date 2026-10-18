package com.solthariel.pawchan.backup

import com.solthariel.pawchan.data.drafts.WorkspaceRecoverySnapshot
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

/** Durable undo, private app storage excluded from Android backup. Presence blocks ordinary writers. */
internal class BackupUndoJournal(
    private val gate: MaintenanceGate,
    private val stores: BackupStores,
    private val directorySync: BackupDirectorySync = AndroidBackupDirectorySync,
) {
    suspend fun restore(backup: ClientBackup) =
        gate.access {
            stores.validate(backup)
            val previous = stores.snapshot(BackupSection.entries.toSet())
            val workspace = stores.workspace.load()
            val undo =
                buildJsonObject {
                    put("backup", Base64.getEncoder().encodeToString(encodeClientBackup(previous)))
                    put("workspace", WorkspaceRecoverySnapshot.encode(workspace))
                }.toString().toByteArray(Charsets.UTF_8)
            write(undo)
            // Once undo is durable, screen cancellation cannot interrupt commit or rollback.
            withContext(NonCancellable) {
                var applied = false
                try {
                    stores.apply(backup)
                    applied = true
                } finally {
                    if (!applied) rollback()
                }
                remove()
            }
        }

    suspend fun recover() =
        gate.recover {
            if (gate.journal.exists()) withContext(NonCancellable) { rollback() }
        }

    private suspend fun rollback() {
        require(gate.journal.length() <= MAX_UNDO_BYTES) { "Журнал восстановления слишком велик" }
        val undo = Json.parseToJsonElement(gate.journal.readText()).jsonObject
        val backup = decodeClientBackup(Base64.getDecoder().decode(undo.getValue("backup").jsonPrimitive.content))
        val workspace = WorkspaceRecoverySnapshot.decode(undo.getValue("workspace").jsonPrimitive.content)
        stores.validate(backup)
        stores.apply(backup)
        stores.workspace.update { workspace }
        remove()
    }

    private fun write(bytes: ByteArray) {
        require(bytes.size <= MAX_UNDO_BYTES) { "Недостаточно допустимого объёма для страховочной копии" }
        val temporary = java.io.File(gate.directory, "pending.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(bytes)
                output.fd.sync()
            }
            Files.move(temporary.toPath(), gate.journal.toPath(), StandardCopyOption.ATOMIC_MOVE)
            syncDirectory()
        } finally {
            temporary.delete()
        }
    }

    private fun remove() {
        Files.delete(gate.journal.toPath())
        syncDirectory()
    }

    private fun syncDirectory() {
        directorySync.sync(gate.directory)
    }
}

private const val MAX_UNDO_BYTES = 160 * 1024 * 1024
