package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal fun catalogFilterHits(
    matched: List<CatalogThreadUi>,
    reasons: Map<ThreadAddress, CatalogAutoHideReason>,
    manuallyHidden: Set<ThreadAddress>,
    revealed: Boolean,
): List<ContentFilterHit> =
    if (revealed) {
        emptyList()
    } else {
        matched.mapNotNull { row ->
            reasons[row.address]?.takeIf { row.address !in manuallyHidden }?.let { reason ->
                ContentFilterHit(reason.ruleId, row.address, row.id, true)
            }
        }
    }

internal fun recordCatalogFilterStatistics(
    scope: CoroutineScope,
    state: StateFlow<CatalogUiState>,
    statistics: ContentFilterStatisticsRepository?,
) {
    if (statistics != null) {
        scope.launch {
            state.collect { published ->
                val hits = (published as? CatalogUiState.Content)?.contentFilters?.hits.orEmpty()
                if (hits.isNotEmpty()) statistics.record(hits)
            }
        }
    }
}
