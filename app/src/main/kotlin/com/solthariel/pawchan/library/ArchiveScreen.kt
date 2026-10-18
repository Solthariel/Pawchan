package com.solthariel.pawchan.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.domain.boards.ArchivedThreadSummary
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
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = actions.onBack) { Text(stringResource(R.string.back_to_catalog)) }
            TextButton(onClick = actions.onRefresh, enabled = !state.loading) { Text(stringResource(R.string.refresh)) }
        }
        Text(
            stringResource(R.string.board_archive_title, board.board.value),
            Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleLarge,
        )
        ArchivePaging(state, actions)
        ArchiveStatus(state)
        key(state.pageIndex) {
            LazyColumn(Modifier.weight(1f)) {
                items(
                    state.snapshot?.threads.orEmpty(),
                    key = { it.address.saveableKey },
                    contentType = { "archive-thread" },
                ) {
                    ArchiveThreadRow(it, actions.onThread)
                }
            }
        }
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
    Row(Modifier.fillMaxWidth()) {
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
private fun ArchiveStatus(state: ArchiveUiState) {
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
}

@Composable
private fun ArchiveThreadRow(
    thread: ArchivedThreadSummary,
    onThread: (ThreadAddress) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable {
                onThread(thread.address)
            }.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(thread.subject, maxLines = 3)
        Text(
            stringResource(R.string.archive_thread_number, thread.address.thread.value),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    HorizontalDivider()
}
