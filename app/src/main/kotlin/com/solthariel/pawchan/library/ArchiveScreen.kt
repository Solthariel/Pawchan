package com.solthariel.pawchan.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.postContainerShape
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.isRateLimited
import com.solthariel.pawchan.navigation.chrome.APP_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.navigation.chrome.BOTTOM_FADE_FRACTION
import com.solthariel.pawchan.navigation.chrome.LocalBottomBarTransparency
import com.solthariel.pawchan.navigation.chrome.bottomBarSurfaceAlpha
import com.solthariel.pawchan.navigation.saveableKey
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ArchiveScreen(
    board: BoardAddress,
    state: ArchiveUiState,
    actions: ArchiveActions,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = APP_TOP_BAR_HEIGHT,
                ).padding(horizontal = com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UiToolbarButton(
                UiIconKind.BACK,
                stringResource(R.string.back_to_catalog).removePrefix("← "),
                onClick = actions.onBack,
            )
            Text(
                stringResource(R.string.board_archive_title, board.board.value),
                Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            UiToolbarButton(
                UiIconKind.REFRESH,
                stringResource(R.string.refresh),
                enabled = !state.loading,
                onClick = actions.onRefresh,
            )
        }
        TransientNotice(
            if (state.rows.isNotEmpty()) archiveStatusMessage(state)?.let { stringResource(it) } else null,
            eventKey = state.loading to state.failure,
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.rows.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    ArchiveStatus(state, actions.onRefresh)
                }
            } else {
                key(state.pageIndex) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = APP_BOTTOM_BAR_HEIGHT + 8.dp),
                    ) {
                        itemsIndexed(state.rows, key = {
                            _,
                            row,
                            ->
                            row.address.saveableKey
                        }, contentType = { _, _ -> "archive-thread" }) { index, row ->
                            ArchiveThreadRow(row, actions.onThread)
                            if (index < state.rows.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }
            ArchivePaging(state, actions, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun ArchivePaging(
    state: ArchiveUiState,
    actions: ArchiveActions,
    modifier: Modifier,
) {
    val res = LocalResources.current

    val pages =
        state.snapshot
            ?.pages
            ?.size
            ?.coerceAtLeast(1) ?: 1
    val surface = MaterialTheme.colorScheme.surface
    val alpha = bottomBarSurfaceAlpha(LocalBottomBarTransparency.current)
    Row(
        modifier
            .fillMaxWidth()
            .height(APP_BOTTOM_BAR_HEIGHT)
            .background(
                Brush.verticalGradient(
                    0f to surface.copy(alpha = 0f),
                    BOTTOM_FADE_FRACTION to surface.copy(alpha = alpha),
                    1f to surface.copy(alpha = alpha),
                ),
            ).padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            TextButton(actions.onPrevious, enabled = !state.loading && state.pageIndex > 0) {
                Text("← " + stringResource(R.string.archive_previous))
            }
        }
        if (state.loading) {
            PageLoadingIndicator(
                description = res.getString(L.ui_loading_archive_page),
                modifier = Modifier.padding(12.dp).testTag("archive-page-loading"),
                size = 20.dp,
            )
        } else {
            Text(stringResource(R.string.archive_page_number, state.pageIndex + 1, pages), Modifier.padding(12.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            TextButton(actions.onNext, enabled = !state.loading && state.pageIndex + 1 < pages) {
                Text(stringResource(R.string.archive_next) + " →")
            }
        }
    }
}

@Composable
private fun ArchiveStatus(
    state: ArchiveUiState,
    retry: () -> Unit,
) {
    val res = LocalResources.current

    val message = archiveStatusMessage(state)
    message?.let { Text(stringResource(it), Modifier.padding(16.dp)) }
    if (state.failure != null && !state.loading) {
        TextButton(onClick = retry) { Text(res.getString(L.ui_retry)) }
    }
}

private fun archiveStatusMessage(state: ArchiveUiState): Int? =
    when {
        state.loading -> R.string.board_information_loading
        state.failure == DomainFailure.NotFound -> R.string.archive_not_available
        state.failure?.isRateLimited == true -> R.string.board_rate_limited
        state.failure != null -> R.string.archive_load_failed
        state.snapshot?.threads?.isEmpty() == true -> R.string.archive_empty
        else -> null
    }

@Composable
private fun ArchiveThreadRow(
    thread: ArchiveThreadPresentation,
    onThread: (ThreadAddress) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onThread(thread.address) },
        shape = postContainerShape(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                thread.subject,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                thread.metadata,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
