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
                    var added = 0
                    var fresh = emptyList<String>()
                    workspace.update { before ->
                        val after = before.observe(outcome.value)
                        val known = before.replies.map { it.post.address }.toSet()
                        fresh = after.replies.filterNot { it.post.address in known }.map { it.post.address.toString() }
                        added = fresh.size
                        after
                    }
                    fresh.forEach {
                        statistics?.record(
                            com.solthariel.pawchan.domain.statistics.UsageMetric.REPLIES_RECEIVED,
                            identity = it,
                        )
                    }
                    if (added > 0) onReplies(address, added)
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
