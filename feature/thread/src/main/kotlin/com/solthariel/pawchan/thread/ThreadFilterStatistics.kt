package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal fun ThreadUiState.Content.filteredThreadPosts(
    engine: ContentFilterEngine,
    revealed: Set<Long>,
    showHiddenPosts: Boolean,
): FilteredThreadPosts {
    val thread = requireNotNull(address)
    val hits = mutableListOf<ContentFilterHit>()
    val omitted = mutableMapOf<Long, ThreadPostUi>()
    val filtered =
        posts.mapNotNull { post ->
            val rule =
                if (post.id == id) {
                    null
                } else {
                    engine.comment(thread.board, post.document.plainText, thread.thread.value)
                }
            val hiddenByRule = rule != null && post.id !in revealed
            if (hiddenByRule && !post.metadata.hidden) {
                hits.add(ContentFilterHit(requireNotNull(rule).id, thread, post.id, false))
            }
            val marked =
                post.copy(
                    metadata =
                        post.metadata.copy(
                            visibility =
                                ThreadPostVisibility(
                                    hidden = post.metadata.hidden || hiddenByRule,
                                    contentFilterLabel = rule?.label?.takeIf { hiddenByRule },
                                    contentFilterId = rule?.id?.takeIf { hiddenByRule },
                                ),
                        ),
                )
            if (!showHiddenPosts && hiddenByRule && !post.metadata.hidden) {
                omitted[post.id] = marked
                null
            } else {
                marked
            }
        }
    return FilteredThreadPosts(filtered, hits, omitted)
}

internal fun recordThreadFilterStatistics(
    scope: CoroutineScope,
    state: StateFlow<ThreadUiState>,
    statistics: ContentFilterStatisticsRepository?,
) {
    if (statistics != null) {
        scope.launch {
            state.collect { published ->
                val hits =
                    (published as? ThreadUiState.Content)
                        ?.presentation
                        ?.contentFilters
                        ?.hits
                        .orEmpty()
                if (hits.isNotEmpty()) statistics.record(hits)
            }
        }
    }
}
