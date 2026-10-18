package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.workspace.WorkspaceState

/** Internal recovery payload, not the portable user backup format. */
object WorkspaceRecoverySnapshot {
    fun encode(state: WorkspaceState): String = encodeWorkspace(state)

    fun decode(source: String): WorkspaceState = decodeWorkspace(source)
}
