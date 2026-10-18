package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.LoadErrorPane
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import com.solthariel.pawchan.core.ui.RefreshFailureNotice
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.media.MediaSaveStatusText
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ThreadWorkspace(
    state: ThreadUiState,
    actions: ThreadActions,
    thumbnails: ThumbnailLoader,
    save: MediaSaveActions,
    memory: ThreadScreenMemory,
    selection: ThreadSelectionActions,
) {
    val content = state as? ThreadUiState.Content
    val tools = memory.tools
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBarsIgnoringVisibility)) {
        ThreadTopBar(state, actions, tools, selection) { memory.goTo(content, it) }
        content?.presentation?.contentFilterNotice?.let { label ->
            androidx.compose.material3.Text(
                "Тред соответствует фильтру: $label",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
        }
        if (content != null && content.presentation.filteredComments > 0) {
            androidx.compose.material3.Text("Скрыто комментариев: ${content.presentation.filteredComments}")
        }
        RefreshFailureNotice(content?.presentation?.refreshFailure, actions.onChallenge)
        MediaSaveStatusText(save.status)
        ThreadBody(
            state,
            actions,
            ThreadContentBindings(thumbnails, {
                memory.opened = it
            }, save.request, {
                memory.mediaMenu =
                    com.solthariel.pawchan.media
                        .MediaMenuRequest(it, com.solthariel.pawchan.media.MediaMenuPlace.THUMBNAIL)
            }, ThreadContentPostActions(actions.onReplyTo, selection.showHidden), tools, memory.appearance),
            memory.list,
        )
    }
}

@Composable
private fun ThreadBody(
    state: ThreadUiState,
    actions: ThreadActions,
    bindings: ThreadContentBindings,
    list: LazyListState,
) {
    BidirectionalPullRefreshBox(
        refreshing = state is ThreadUiState.Loading || (state as? ThreadUiState.Content)?.isRefreshing == true,
        onRefresh = actions.onRefresh,
        options =
            PullRefreshOptions(
                allowBottom = true,
                bottomInset = LocalBottomOverlayInset.current.let { if (it > 0.dp) it + 6.dp else it },
            ),
        modifier = Modifier.fillMaxSize(),
    ) {
        when (state) {
            ThreadUiState.Loading -> {
                ThreadLoadingSkeleton()
            }

            is ThreadUiState.Content -> {
                ThreadContent(state, list, bindings)
            }

            is ThreadUiState.Error -> {
                LoadErrorPane(
                    challengeUrl = state.challengeUrl,
                    rateLimited =
                        state.reason == ThreadError.RATE_LIMITED,
                    onChallenge = actions.onChallenge,
                )
            }
        }
    }
}
