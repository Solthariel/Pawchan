package com.solthariel.pawchan.workspace

import android.content.Context
import com.solthariel.pawchan.data.drafts.FileWorkspaceRepository
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.observe
import java.io.File
import java.io.IOException

internal suspend fun checkWorkspaceReplies(
    context: Context,
    repository: ThreadRepository,
): Boolean = checkWorkspaceReplies(FileWorkspaceRepository(File(context.filesDir, "workspace")), repository)

internal suspend fun checkWorkspaceReplies(
    workspace: WorkspaceRepository,
    repository: ThreadRepository,
    statistics: com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository? = null,
    synchronization: ReplyNotificationSync? = null,
    onReplies: (ThreadAddress, Int) -> Unit = { _, _ -> },
): Boolean =
    try {
        val threads =
            workspace
                .load()
                .posts
                .filter { it.own && it.tracking }
                .map { it.post.address.thread }
                .distinct()
        var success = true
        for (address in threads) {
            when (val outcome = repository.loadThread(address)) {
                is DomainOutcome.Success -> {
                    val fresh = recordDiscoveredReplies(workspace, outcome.value, synchronization, onReplies)
                    fresh.forEach {
                        statistics?.record(
                            com.solthariel.pawchan.domain.statistics.UsageMetric.REPLIES_RECEIVED,
                            identity = it,
                        )
                    }
                }

                is DomainOutcome.Failure -> {
                    if (outcome.reason == DomainFailure.NotFound) {
                        workspace.update { state ->
                            state.copy(
                                posts =
                                    state.posts.map {
                                        if (it.post.address.thread == address) it.copy(tracking = false) else it
                                    },
                            )
                        }
                    } else {
                        success = false
                    }
                }
            }
        }
        success
    } catch (_: IOException) {
        false
    }

private suspend fun recordDiscoveredReplies(
    workspace: WorkspaceRepository,
    snapshot: com.solthariel.pawchan.domain.model.ThreadSnapshot,
    synchronization: ReplyNotificationSync?,
    onReplies: (ThreadAddress, Int) -> Unit,
): List<String> {
    var fresh = emptyList<String>()
    val saveAndNotify: suspend () -> Unit = {
        val after =
            workspace.update { before ->
                val updated = before.observe(snapshot)
                val known = before.replies.map { it.post.address }.toSet()
                fresh = updated.replies.filterNot { it.post.address in known }.map { it.post.address.toString() }
                updated
            }
        if (fresh.isNotEmpty()) {
            val unread = after.replies.count { it.unread && it.post.address.thread == snapshot.address }
            if (unread > 0) onReplies(snapshot.address, unread)
        }
    }
    if (synchronization == null) saveAndNotify() else synchronization.access(saveAndNotify)
    return fresh
}
