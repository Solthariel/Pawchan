package com.solthariel.pawchan.workspace

import android.content.Context
import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.watch.ThreadWatchNotifier
import java.io.File

/** Serializes reply state and system notification updates across UI and workers. */
internal class ReplyNotificationSync(
    private val gate: MaintenanceGate,
    private val update: (ThreadAddress, Int) -> Unit,
) {
    constructor(context: Context) : this(
        MaintenanceGate(File(context.filesDir, "reply-notification-sync")),
        ThreadWatchNotifier(context, replies = true)::updateRepliesIfActive,
    )

    suspend fun <T> access(block: suspend () -> T): T = gate.access(block)

    fun reconcile(
        address: ThreadAddress,
        state: WorkspaceState,
    ) {
        val unread = state.replies.count { it.unread && it.post.address.thread == address }
        update(address, unread)
    }
}
