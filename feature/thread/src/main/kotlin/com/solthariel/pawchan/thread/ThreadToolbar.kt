package com.solthariel.pawchan.thread

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.ThreadToolbarActionLayout
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiRefreshButton
import com.solthariel.pawchan.feature.thread.R
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun ThreadTopBar(
    state: ThreadUiState,
    actions: ThreadActions,
    tools: ThreadToolsState,
    selection: ThreadSelectionActions,
    onSearchResult: (Int) -> Unit,
) {
    if (tools.selecting) {
        ThreadSelectionBar(tools, selection)
    } else {
        ThreadToolbar(state, actions, tools, onSearchResult)
    }
}

@Composable
internal fun ThreadToolbar(
    state: ThreadUiState,
    actions: ThreadActions,
    tools: ThreadToolsState,
    onSearchResult: (Int) -> Unit,
) {
    val content = state as? ThreadUiState.Content
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = APP_TOP_BAR_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = tools.searchOpen,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                        slideOutHorizontally { -it } + fadeOut(),
                    )
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(
                        slideOutHorizontally { it } + fadeOut(),
                    )
                }
            },
            label = "thread-search",
        ) { searchOpen ->
            if (searchOpen) {
                ThreadSearchBar(tools, content, onSearchResult)
            } else {
                ThreadToolbarActions(state, content, actions, tools)
            }
        }
    }
}

@Composable
private fun ThreadToolbarActions(
    state: ThreadUiState,
    content: ThreadUiState.Content?,
    actions: ThreadActions,
    tools: ThreadToolsState,
) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = threadToolbarUsesCompactActions(maxWidth)
        ThreadToolbarActionRow(state, content, actions, tools, compact)
    }
}

@Composable
private fun ThreadToolbarActionRow(
    state: ThreadUiState,
    content: ThreadUiState.Content?,
    actions: ThreadActions,
    tools: ThreadToolsState,
    compact: Boolean,
) {
    val res = LocalResources.current

    ThreadToolbarActionLayout(
        leading = { ThreadToolbarNavigation(content, actions, tools, compact) },
        trailing = {
            ThreadToolbarButton(
                kind = UiIconKind.SEARCH,
                description = res.getString(L.ui_search),
                enabled = content != null,
                onClick = { tools.searchOpen = true },
            )
            ThreadToolbarSlot {
                UiRefreshButton(
                    description = stringResource(R.string.refresh),
                    isRefreshing = content?.isRefreshing == true || state is ThreadUiState.Loading,
                    enabled = content?.isRefreshing != true && state !is ThreadUiState.Loading,
                    onClick = actions.onRefresh,
                )
            }
            ThreadToolbarSlot {
                Box {
                    ThreadToolbarButton(
                        kind = UiIconKind.MORE,
                        description = res.getString(L.ui_thread_menu),
                        enabled = content != null,
                        onClick = { tools.menu = true },
                    )
                    ThreadOverflowMenu(tools, content, actions, compact)
                }
            }
        },
    )
}

@Composable
private fun ThreadToolbarSlot(content: @Composable () -> Unit) {
    Box(contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun ThreadToolbarNavigation(
    content: ThreadUiState.Content?,
    actions: ThreadActions,
    tools: ThreadToolsState,
    compact: Boolean,
) {
    val res = LocalResources.current

    ThreadToolbarSlot {
        ThreadToolbarButton(UiIconKind.BACK, res.getString(L.ui_back), onClick = actions.onBack)
    }
    if (compact) return
    ThreadToolbarSlot {
        ThreadToolbarButton(
            kind = if (content?.isFavorite == true) UiIconKind.BOOKMARK_FILLED else UiIconKind.BOOKMARK_ADD,
            description =
                stringResource(
                    if (content?.isFavorite == true) R.string.favorite_remove else R.string.favorite_add,
                ),
            enabled = content != null,
            active = content?.isFavorite == true,
            onClick = actions.onFavoriteToggle,
        )
    }
    ThreadToolbarButton(
        kind = UiIconKind.MEDIA_MULTIPLE,
        description = res.getString(L.ui_gallery),
        enabled = content != null,
        onClick = { tools.dialog = ThreadDialog.GALLERY },
    )
}
