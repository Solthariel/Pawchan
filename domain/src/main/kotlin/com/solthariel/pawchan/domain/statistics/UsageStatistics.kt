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
    /** Hashed object IDs and weights; values includes these objects plus any legacy aggregate. */
    val objects: Map<UsageMetric, Map<String, Long>> = emptyMap(),
)

interface UsageStatisticsRepository {
    val days: Flow<List<UsageDay>>
    val available: Flow<Boolean>

    /** Positive records require an object identity; only byte volume may have a weight other than one. */
    suspend fun record(
        metric: UsageMetric,
        amount: Long = 1L,
        identity: String? = null,
    )

    suspend fun reset()
}
