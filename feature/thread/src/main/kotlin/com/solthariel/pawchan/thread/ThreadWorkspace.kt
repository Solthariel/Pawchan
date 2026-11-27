package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.LoadErrorPane
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import com.solthariel.pawchan.core.ui.RefreshFailureNotice
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.feature.thread.R
import com.solthariel.pawchan.media.MediaMenuPlace
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.feature.thread.R.string as L

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ThreadWorkspace(
    state: ThreadUiState,
    actions: ThreadActions,
    bindings: ThreadContentBindings,
    memory: ThreadScreenMemory,
    selection: ThreadSelectionActions,
    autoRefreshFailed: Boolean = false,
) {
    val res = LocalResources.current

    val content = state as? ThreadUiState.Content
    val tools = memory.tools
    val insets =
        if (com.solthariel.pawchan.core.ui.LocalFloatingTopEnabled.current) {
            WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Horizontal)
        } else if (LocalBottomOverlayInset.current > 0.dp) {
            WindowInsets.systemBarsIgnoringVisibility.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
        } else {
            WindowInsets.systemBarsIgnoringVisibility
        }
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().windowInsetsPadding(insets)) {
        com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
            ThreadTopBar(state, actions, tools, selection) { memory.moveSearch(content, it) }
            val filterNotice =
                content?.presentation?.let { presentation ->
                    listOfNotNull(
                        presentation.contentFilterNotice?.let { res.getString(L.ui_thread_matches_filter, it) },
                        res
                            .getString(L.ui_hidden_comments, presentation.filteredComments)
                            .takeIf { presentation.filteredComments > 0 },
                    ).joinToString("\n").takeIf(String::isNotEmpty)
                }
            if (autoRefreshFailed && content != null && content.presentation.refreshFailure == null) {
                androidx.compose.material3.Text(stringResource(R.string.watcher_error))
            }
            TransientNotice(filterNotice)
            RefreshFailureNotice(content?.presentation?.refreshFailure, actions.onChallenge)
        }) {
            ThreadBody(state, actions, bindings, memory.list)
        }
    }
}

internal fun ThreadScreenMemory.contentBindings(
    thumbnails: ThumbnailLoader,
    save: MediaSaveActions,
    actions: ThreadActions,
    selection: ThreadSelectionActions,
): ThreadContentBindings =
    ThreadContentBindings(
        loader = thumbnails,
        onOpen = { opened = it },
        onDownload = save.request,
        onMediaMenu = { mediaMenu = MediaMenuRequest(it, MediaMenuPlace.THUMBNAIL) },
        postActions = ThreadContentPostActions(actions.onReplyTo, selection.showHidden),
        tools = tools,
        appearance = appearance,
    )

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
                    message =
                        if (state.reason == ThreadError.NOT_FOUND) {
                            stringResource(R.string.thread_not_exists)
                        } else {
                            null
                        },
                    challengeUrl = state.challengeUrl,
                    rateLimited =
                        state.reason == ThreadError.RATE_LIMITED,
                    onChallenge = actions.onChallenge,
                )
            }
        }
    }
}
