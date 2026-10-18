package com.solthariel.pawchan.domain.statistics

import kotlinx.coroutines.flow.Flow

enum class UsageMetric {
    THREADS_OPENED,
    POSTS_VIEWED,
    POSTS_SENT,
    THREADS_CREATED,
    REPLIES_SENT,
    REPLIES_RECEIVED,
    IMAGES_SENT,
    VIDEOS_SENT,
    IMAGES_OPENED,
    VIDEOS_OPENED,
    FILES_DOWNLOADED,
    BYTES_DOWNLOADED,
    THREADS_FAVORITED,
    THREADS_HIDDEN,
    THREADS_AUTO_HIDDEN,
    STICKERS_HIDDEN,
}

data class UsageDay(
    val epochDay: Long,
    val values: Map<UsageMetric, Long>,
)

interface UsageStatisticsRepository {
    val days: Flow<List<UsageDay>>
    val available: Flow<Boolean>

    suspend fun record(
        metric: UsageMetric,
        amount: Long = 1L,
        identity: String? = null,
    )

    suspend fun reset()
}
