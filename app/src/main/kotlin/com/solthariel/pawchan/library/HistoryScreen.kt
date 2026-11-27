package com.solthariel.pawchan.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.navigation.saveableKey
import com.solthariel.pawchan.threads.ThreadSourceIcon
import com.solthariel.pawchan.threads.ThreadSourceUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.solthariel.pawchan.R.string as L

/** История просмотренных тредов одной строкой, последние — сверху. */
@Composable
internal fun HistoryScreen(
    state: LibraryUiState,
    onBack: () -> Unit,
    onThreadClick: (ThreadAddress) -> Unit,
    bottomActions: AppBottomActions,
    onHistoryCleared: () -> Unit = {},
    sources: Map<SiteId, ThreadSourceUi> = emptyMap(),
) {
    val res = LocalResources.current

    var query by rememberSaveable { mutableStateOf<String?>(null) }
    var refreshNotice by remember { mutableStateOf<String?>(null) }
    val refreshFailures = (state as? LibraryUiState.Content)?.refreshFailures ?: 0
    LaunchedEffect(refreshFailures) {
        if (refreshFailures > 0) {
            refreshNotice = res.getString(L.ui_failed_to_update_history)
        }
    }
    val entries =
        remember(state, query) {
            (state as? LibraryUiState.Content)
                ?.entries
                ?.filter { it.lastVisitedAtEpochMillis > 0 && it.matches(query.orEmpty().trim()) }
                ?.sortedByDescending { it.lastVisitedAtEpochMillis }
        }
    AppNavigationWorkspace(actions = bottomActions, selected = AppBottomDestination.THREADS, floatingTop = true) {
        com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
            LibraryToolbar(
                LibraryToolbarState(res.getString(L.ui_history), entries?.size, query, false),
                LibraryToolbarActions(
                    onBack = onBack,
                    onQuery = { query = it },
                    history = true,
                    onHistoryCleared = onHistoryCleared,
                ),
            )
        }) {
            when {
                state == LibraryUiState.Error ||
                    (
                        state is LibraryUiState.Content && state.refreshFailures > 0 &&
                            state.entries.none { it.lastVisitedAtEpochMillis > 0 }
                    ) -> {
                    HistoryEmpty(res.getString(L.ui_error_loading_data))
                }

                entries == null -> {
                    HistoryEmpty(res.getString(L.ui_loading))
                }

                entries.isEmpty() -> {
                    HistoryEmpty(res.getString(L.ui_history_empty))
                }

                else -> {
                    HistoryList(entries, onThreadClick, sources)
                }
            }
        }
    }
    TransientNotice(
        refreshNotice,
        eventKey = refreshFailures,
        onDismiss = { refreshNotice = null },
    )
}

@Composable
private fun HistoryEmpty(message: String) {
    Box(Modifier.padding(start = 8.dp, top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current)) {
        LibraryEmpty(message)
    }
}

@Composable
private fun HistoryList(
    entries: List<LibraryEntryUi>,
    onThreadClick: (ThreadAddress) -> Unit,
    sources: Map<SiteId, ThreadSourceUi>,
) {
    val res = LocalResources.current

    val list = rememberLazyListState()
    val format = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                    bottom = com.solthariel.pawchan.core.ui.LocalBottomOverlayInset.current + 8.dp,
                ),
        ) {
            items(entries, key = { it.address.saveableKey }, contentType = { "history-row" }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onThreadClick(entry.address) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ThreadSourceIcon(
                        source =
                            sources[entry.address.board.site] ?: ThreadSourceUi(entry.address.board.site.value),
                        current = false,
                    )
                    Text(
                        entry.subject?.takeIf(String::isNotBlank) ?: res.getString(L.ui_thread_no, entry.threadId),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        format.format(Date(entry.lastVisitedAtEpochMillis)),
                        Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        PawchanFastScrollbar(state = list, modifier = Modifier.align(Alignment.CenterEnd))
    }
}
