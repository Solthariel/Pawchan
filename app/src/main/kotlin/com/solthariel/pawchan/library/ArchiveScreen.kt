package com.solthariel.pawchan.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiThreadCard
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiTopAppBar
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.isRateLimited
import com.solthariel.pawchan.navigation.saveableKey

@Composable
internal fun ArchiveScreen(
    board: BoardAddress,
    state: ArchiveUiState,
    actions: ArchiveActions,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        UiTopAppBar(
            title = {
                Text(
                    stringResource(R.string.board_archive_title, board.board.value),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            navigationIcon = {
                UiToolbarButton(
                    UiIconKind.BACK,
                    stringResource(R.string.back_to_catalog).removePrefix("← "),
                    onClick = actions.onBack,
                )
            },
            actions = {
                UiToolbarButton(
                    UiIconKind.REFRESH,
                    stringResource(R.string.refresh),
                    enabled = !state.loading,
                    onClick = actions.onRefresh,
                )
            },
        )
        ArchiveStatus(state, actions.onRefresh)
        key(state.pageIndex) {
            LazyColumn(Modifier.weight(1f)) {
                items(
                    state.rows,
                    key = { it.address.saveableKey },
                    contentType = { "archive-thread" },
                ) {
                    ArchiveThreadRow(it, actions.onThread)
                }
            }
        }
        HorizontalDivider()
        ArchivePaging(state, actions)
    }
}

@Composable
private fun ArchivePaging(
    state: ArchiveUiState,
    actions: ArchiveActions,
) {
    val pages =
        state.snapshot
            ?.pages
            ?.size
            ?.coerceAtLeast(1) ?: 1
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(actions.onPrevious, enabled = !state.loading && state.pageIndex > 0) {
            Text(stringResource(R.string.archive_previous))
        }
        Text(stringResource(R.string.archive_page_number, state.pageIndex + 1, pages), Modifier.padding(12.dp))
        TextButton(actions.onNext, enabled = !state.loading && state.pageIndex + 1 < pages) {
            Text(stringResource(R.string.archive_next))
        }
    }
}

@Composable
private fun ArchiveStatus(
    state: ArchiveUiState,
    retry: () -> Unit,
) {
    val message =
        when {
            state.loading -> R.string.board_information_loading
            state.failure == DomainFailure.NotFound -> R.string.archive_not_available
            state.failure?.isRateLimited == true -> R.string.board_rate_limited
            state.failure != null -> R.string.archive_load_failed
            state.snapshot?.threads?.isEmpty() == true -> R.string.archive_empty
            else -> null
        }
    message?.let { Text(stringResource(it), Modifier.padding(16.dp)) }
    if (state.failure != null && !state.loading) {
        TextButton(onClick = retry) { Text("Повторить") }
    }
}

@Composable
private fun ArchiveThreadRow(
    thread: ArchiveThreadPresentation,
    onThread: (ThreadAddress) -> Unit,
) {
    val bubble = com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences.current.bubblePosts
    UiThreadCard(
        outlined = bubble,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (bubble) 8.dp else 0.dp, vertical = if (bubble) 4.dp else 0.dp)
                .clickable { onThread(thread.address) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
            Text(
                thread.subject,
                style = MaterialTheme.typography.titleMedium,
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
    if (!bubble) HorizontalDivider()
}
