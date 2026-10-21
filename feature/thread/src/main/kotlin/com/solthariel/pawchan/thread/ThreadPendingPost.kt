package com.solthariel.pawchan.thread

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.solthariel.pawchan.domain.model.ThreadAddress

/** Keep the current viewport while a notification target is absent from cached content. */
@Composable
internal fun BindThreadPendingPost(
    memory: ThreadScreenMemory,
    content: ThreadUiState.Content?,
    address: ThreadAddress?,
    reading: ThreadReadingSession?,
    requestPost: suspend (Long) -> Unit,
) {
    val target = address?.let { reading?.pendingPost(it) }
    val index = target?.let { content?.presentation?.postPositions?.get(it) }
    LaunchedEffect(reading, address, target, index, memory.list) {
        if (address != null && target != null) {
            if (index != null) {
                memory.list.scrollToItem(index)
                reading?.clearPendingPost(address)
            } else if (content != null) {
                requestPost(target)
            }
        }
    }
}
