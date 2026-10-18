package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Same-process and cross-process writers use the existing directory lock and atomic replacement. */
class FileWorkspaceRepository(
    private val directory: File,
) : WorkspaceRepository {
    private val gate = MaintenanceGate(File(directory.parentFile, "backup-maintenance"))
    private val index = File(directory, "workspace.json")

    override suspend fun load(): WorkspaceState =
        withContext(Dispatchers.IO) {
            gate.access { withDraftDirectoryLock(directory) { read() } }
        }

    override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState =
        withContext(Dispatchers.IO) {
            gate.access {
                withDraftDirectoryLock(directory) {
                    val updated = transform(read())
                    val bytes = encodeWorkspace(updated).toByteArray()
                    if (bytes.size >
                        MAX_WORKSPACE_BYTES
                    ) {
                        throw IOException("Workspace exceeds 64 MiB; remove unused copies")
                    }
                    atomicWrite(index, bytes)
                    updated
                }
            }
        }

    private fun read(): WorkspaceState {
        if (!index.exists()) return WorkspaceState()
        if (index.length() > MAX_WORKSPACE_BYTES) throw IOException("Workspace exceeds safety limit")
        return try {
            decodeWorkspace(index.readText())
        } catch (failure: IllegalArgumentException) {
            throw IOException("Saved workspace is damaged; original file was not overwritten", failure)
        }
    }
}

private const val MAX_WORKSPACE_BYTES = 64 * 1024 * 1024
