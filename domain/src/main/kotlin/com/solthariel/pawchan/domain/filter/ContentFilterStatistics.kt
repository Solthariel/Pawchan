package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.flow.Flow

/** One actual hide, not an engine evaluation, probe, preview or manual hiding action. */
data class ContentFilterHit(
    val ruleId: String,
    val thread: ThreadAddress,
    val postId: Long,
    val originalPost: Boolean,
) {
    init {
        require(ruleId.isNotBlank() && postId > 0L)
    }
}

data class ContentFilterBoardStatistics(
    val site: String,
    val board: String,
    val originalPosts: Long,
    val replies: Long,
    val lastHitMillis: Long,
) {
    val total: Long get() = originalPosts + replies
}

data class ContentFilterStatistics(
    val rules: Map<String, Long> = emptyMap(),
    val boards: List<ContentFilterBoardStatistics> = emptyList(),
    val available: Boolean = true,
    val storageFailure: Boolean = false,
) {
    val total: Long get() = boards.sumOf { it.total }
    val originalPosts: Long get() = boards.sumOf { it.originalPosts }
    val replies: Long get() = boards.sumOf { it.replies }
}

interface ContentFilterStatisticsRepository {
    val statistics: Flow<ContentFilterStatistics>

    suspend fun record(hits: List<ContentFilterHit>): DomainOutcome<Unit>

    suspend fun reset(): DomainOutcome<Unit>
}
