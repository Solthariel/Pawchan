package com.solthariel.pawchan.infrastructure

import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository

/** Only catalog hides count as hidden threads. Identity deduplicates rules and reloads per day. */
internal class UsageFilterStatistics(
    private val delegate: ContentFilterStatisticsRepository,
    private val usage: UsageStatisticsRepository,
) : ContentFilterStatisticsRepository by delegate {
    override suspend fun record(hits: List<ContentFilterHit>): DomainOutcome<Unit> {
        val result = delegate.record(hits)
        if (result is DomainOutcome.Success) {
            hits.filter { it.originalPost }.distinctBy { it.thread }.forEach {
                usage.record(UsageMetric.THREADS_AUTO_HIDDEN, identity = it.thread.toString())
            }
        }
        return result
    }
}
