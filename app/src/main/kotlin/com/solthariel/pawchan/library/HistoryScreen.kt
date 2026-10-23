package com.solthariel.pawchan.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.navigation.chrome.APP_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.navigation.saveableKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** История просмотренных тредов одной строкой, последние — сверху. */
@Composable
internal fun HistoryScreen(
    state: LibraryUiState,
    onBack: () -> Unit,
    onThreadClick: (ThreadAddress) -> Unit,
    bottomActions: AppBottomActions,
    onHistoryCleared: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf<String?>(null) }
    val entries =
        remember(state, query) {
            (state as? LibraryUiState.Content)
                ?.entries
                ?.filter { it.lastVisitedAtEpochMillis > 0 && it.matches(query.orEmpty().trim()) }
                ?.sortedByDescending { it.lastVisitedAtEpochMillis }
        }
    AppNavigationWorkspace(actions = bottomActions, selected = AppBottomDestination.THREADS) {
        Column(Modifier.fillMaxSize()) {
            LibraryToolbar(
                LibraryToolbarState("История", entries?.size, query, false),
                LibraryToolbarActions(
                    onBack = onBack,
                    onQuery = { query = it },
                    history = true,
                    onHistoryCleared = onHistoryCleared,
                ),
            )
            when {
                state == LibraryUiState.Error -> LibraryEmpty("Не удалось загрузить историю")
                entries == null -> LibraryEmpty("Загрузка…")
                entries.isEmpty() -> LibraryEmpty("История пуста")
                else -> HistoryList(entries, onThreadClick)
            }
        }
    }
}

@Composable
private fun HistoryList(
    entries: List<LibraryEntryUi>,
    onThreadClick: (ThreadAddress) -> Unit,
) {
    val list = rememberLazyListState()
    val format = remember { SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = APP_BOTTOM_BAR_HEIGHT + 8.dp),
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
                    Text(
                        entry.subject?.takeIf(String::isNotBlank) ?: "Тред №${entry.threadId}",
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
