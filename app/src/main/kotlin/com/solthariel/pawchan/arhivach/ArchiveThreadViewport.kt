package com.solthariel.pawchan.arhivach

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoading
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

internal data class ArchiveThreadViewport(
    val shown: List<ArchiveThreadCardUi>,
    val listState: LazyListState,
    val thumbnailLoading: ThumbnailLoading,
    val autoLoadPages: Boolean,
)

internal fun ArchiveBrowseState.shouldAutoLoad(
    shownCount: Int,
    autoLoadPages: Boolean,
): Boolean {
    val unfiltered = autoLoadPages && shownCount == threadCards.size
    val pageReady = list?.hasNext == true && !loadingNextPage && nextPageError == null
    return unfiltered && pageReady
}

@Composable
internal fun ArchiveAutoLoadEffect(
    state: ArchiveBrowseState,
    viewport: ArchiveThreadViewport,
    onLoadNextPage: () -> Unit,
) {
    LaunchedEffect(
        viewport.listState,
        viewport.shown.size,
        state.threadCards.size,
        state.list?.hasNext,
        state.loadingNextPage,
        state.nextPageError,
        viewport.autoLoadPages,
    ) {
        if (!state.shouldAutoLoad(viewport.shown.size, viewport.autoLoadPages)) return@LaunchedEffect
        snapshotFlow {
            viewport.listState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index ?: -1
        }.map { lastVisibleIndex ->
            lastVisibleIndex >= (viewport.shown.size - ARCHIVE_PAGE_PREFETCH_DISTANCE).coerceAtLeast(0)
        }.distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd) onLoadNextPage()
            }
    }
}

private const val ARCHIVE_PAGE_PREFETCH_DISTANCE = 4
