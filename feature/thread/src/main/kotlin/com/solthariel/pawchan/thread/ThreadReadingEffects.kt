package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.solthariel.pawchan.domain.model.ThreadAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

@OptIn(kotlinx.coroutines.FlowPreview::class)
@Composable
internal fun BindThreadReadingPosition(
    memory: ThreadScreenMemory,
    content: ThreadUiState.Content?,
    address: ThreadAddress?,
    reading: ThreadReadingSession?,
) {
    if (reading == null) return
    val latest by rememberUpdatedState(content)
    LaunchedEffect(address, reading, content != null) {
        if (address == null || content == null) return@LaunchedEffect
        snapshotFlow {
            memory.list.layoutInfo.visibleItemsInfo
                .filter { item ->
                    item.offset + item.size > memory.list.layoutInfo.viewportStartOffset &&
                        item.offset < memory.list.layoutInfo.viewportEndOffset
                }.mapNotNull { latest?.posts?.getOrNull(it.index)?.id }
        }.collectViewedPosts(address, reading)
    }
    LaunchedEffect(address, content != null, reading) {
        if (address == null || content == null) return@LaunchedEffect
        val positions =
            snapshotFlow {
                latest?.posts?.getOrNull(memory.list.firstVisibleItemIndex)?.let {
                    ThreadReadingPosition(it.id, memory.list.firstVisibleItemScrollOffset)
                }
            }.filterNotNull()
        reading.collectPositions(address, positions)
    }
}

@OptIn(kotlinx.coroutines.FlowPreview::class)
internal suspend fun ThreadReadingSession.collectPositions(
    address: ThreadAddress,
    positions: Flow<ThreadReadingPosition>,
) {
    contentReady(address)
    positions
        .distinctUntilChanged()
        .onEach { visible(address, it) }
        .debounce(POSITION_SAVE_DELAY_MILLIS)
        .collect { persist(address, it) }
}

private const val POSITION_SAVE_DELAY_MILLIS = 300L

@OptIn(kotlinx.coroutines.FlowPreview::class)
internal suspend fun Flow<List<Long>>.collectViewedPosts(
    address: ThreadAddress,
    reading: ThreadReadingSession,
) {
    val pending = linkedSetOf<Long>()
    try {
        distinctUntilChanged()
            .onEach { pending.addAll(it) }
            .debounce(POSITION_SAVE_DELAY_MILLIS)
            .collect {
                if (pending.isNotEmpty()) reading.viewed(address, pending.toList())
                pending.clear()
            }
    } finally {
        if (pending.isNotEmpty()) reading.viewed(address, pending.toList())
    }
}
