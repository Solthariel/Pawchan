package com.solthariel.pawchan.arhivach

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ArchiveThreadListState(
    val listState: LazyListState,
    val shown: List<ArchiveThreadCardUi>,
)

/** Вызывается над условной веткой списка и треда, чтобы сохранять позицию при возврате. */
@Composable
internal fun rememberArchiveThreadListState(
    feedKey: Int,
    cards: List<ArchiveThreadCardUi>,
    words: String,
): ArchiveThreadListState {
    val listState = rememberLazyListState()
    var previousFeedKey by rememberSaveable { mutableIntStateOf(feedKey) }
    LaunchedEffect(feedKey) {
        if (previousFeedKey != feedKey) {
            listState.scrollToItem(0)
            previousFeedKey = feedKey
        }
    }
    var filtered by remember(feedKey) { mutableStateOf(emptyList<ArchiveThreadCardUi>()) }
    LaunchedEffect(feedKey, cards, words) {
        val query = words.trim()
        filtered =
            if (query.isEmpty()) {
                emptyList()
            } else {
                withContext(Dispatchers.Default) {
                    cards.filter { thread ->
                        listOf(
                            thread.summary.id.toString(),
                            thread.summary.title,
                            thread.summary.metadata.previewText,
                            thread.summary.metadata.status
                                .orEmpty(),
                            thread.tags,
                        ).any { it.contains(query, ignoreCase = true) }
                    }
                }
            }
    }
    return ArchiveThreadListState(listState, if (words.isBlank()) cards else filtered)
}
