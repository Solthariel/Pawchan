package com.solthariel.pawchan.thread

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.domain.model.ThreadAddress

/** False while an application-owned full-screen overlay covers the thread. */
val LocalThreadReadingEnabled = staticCompositionLocalOf { true }

/** Only actual, unobstructed list visibility may consume an unread reply. */
@Composable
internal fun BindVisibleReplies(
    memory: ThreadScreenMemory,
    content: ThreadUiState.Content?,
    address: ThreadAddress?,
    reading: ThreadReadingSession?,
) {
    if (address == null || reading == null || content == null) return
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val lifecycleState by lifecycle.currentStateFlow.collectAsState()
    val focused = LocalWindowInfo.current.isWindowFocused
    val bottomInset = with(LocalDensity.current) { LocalBottomOverlayInset.current.roundToPx() }
    val unobstructed =
        LocalThreadReadingEnabled.current && focused && lifecycleState.isAtLeast(Lifecycle.State.RESUMED) &&
            memory.canReadVisiblePosts()
    val latest by rememberUpdatedState(content)
    val minimumPixels = with(LocalDensity.current) { 48.dp.roundToPx() }
    LaunchedEffect(memory.list, address, reading, unobstructed, bottomInset, minimumPixels) {
        if (!unobstructed) return@LaunchedEffect
        val visible =
            snapshotFlow {
                if (memory.list.isScrollInProgress) {
                    emptySet()
                } else {
                    visibleReplyItems(memory.list.layoutInfo, latest, bottomInset, minimumPixels)
                }
            }
        collectVisibleReplies(visible, reading.unreadReplies(address)) { ids ->
            reading.markVisibleReplies(address, ids)
        }
    }
}

private fun ThreadScreenMemory.canReadVisiblePosts(): Boolean =
    opened == null && mediaMenu == null && tools.dialog == ThreadDialog.NONE &&
        tools.postMenuId == null && tools.replyPreviewPostId == null && tools.previews.isEmpty() &&
        !tools.menu && !tools.advanced && !tools.contents

private fun visibleReplyItems(
    layout: LazyListLayoutInfo,
    content: ThreadUiState.Content,
    bottomInset: Int,
    minimumPixels: Int,
): Set<Long> {
    val end = (layout.viewportEndOffset - bottomInset).coerceAtLeast(layout.viewportStartOffset)
    return layout.visibleItemsInfo
        .filter { item ->
            replyIsVisible(item.offset, item.size, layout.viewportStartOffset, end, minimumPixels)
        }.mapNotNull { it.key as? Long }
        .filter { id ->
            content.presentation.postPositions[id]?.let { index ->
                content.posts
                    .getOrNull(index)
                    ?.metadata
                    ?.hidden == false
            } == true
        }.toSet()
}
