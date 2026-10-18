package com.solthariel.pawchan.thread

import com.solthariel.pawchan.core.ui.formatByteSizeForUi

internal data class ThreadMenuStatisticsUi(
    val posts: String,
    val files: String,
    val size: String,
    val participants: String,
    /** null — удалённых постов нет, строка не показывается. */
    val deleted: String? = null,
)

internal fun threadMenuStatistics(content: ThreadUiState.Content): ThreadMenuStatisticsUi {
    val size = formatByteSizeForUi(content.mediaBytes)
    return ThreadMenuStatisticsUi(
        posts = content.posts.count { !it.metadata.changes.deleted }.toString(),
        files = content.files.toString(),
        size = size,
        participants =
            content.statistics.uniquePosters
                .takeIf { it > 0 }
                ?.toString() ?: "—",
        deleted =
            content.posts
                .count { it.metadata.changes.deleted }
                .takeIf { it > 0 }
                ?.toString(),
    )
}
