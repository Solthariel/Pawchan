package com.solthariel.pawchan.workspace

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState

internal suspend fun recordWorkspaceChanges(
    stats: UsageStatisticsRepository?,
    before: WorkspaceState,
    after: WorkspaceState,
) {
    if (stats == null) return
    val known = before.replies.map { it.post.address }.toSet()
    after.replies.filterNot { it.post.address in known }.forEach {
        stats.record(UsageMetric.REPLIES_RECEIVED, identity = it.post.address.toString())
    }
    stats.record(UsageMetric.THREADS_HIDDEN, (after.marks.hidden - before.marks.hidden).size.toLong())
}
