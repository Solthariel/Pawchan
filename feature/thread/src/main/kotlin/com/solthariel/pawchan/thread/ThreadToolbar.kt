package com.solthariel.pawchan.thread

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiRefreshButton
import com.solthariel.pawchan.feature.thread.R

@Composable
internal fun ThreadTopBar(
    state: ThreadUiState,
    actions: ThreadActions,
    tools: ThreadToolsState,
    selection: ThreadSelectionActions,
    onSearchResult: (Long) -> Unit,
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
    onSearchResult: (Long) -> Unit,
) {
    val content = state as? ThreadUiState.Content
    Box(
        modifier = Modifier.fillMaxWidth().height(APP_TOP_BAR_HEIGHT),
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
    Row(
        Modifier
            .fillMaxWidth()
            .height(APP_TOOLBAR_BUTTON_SIZE)
            .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadToolbarNavigation(content, actions, tools)
        Spacer(Modifier.weight(1f))
        ThreadToolbarButton(
            kind = UiIconKind.WRITE,
            description = stringResource(R.string.reply_open),
            enabled = content != null && !content.closed,
            onClick = { actions.onReplyTo(null) },
        )
        ThreadToolbarButton(
            kind = UiIconKind.SEARCH,
            description = "Поиск",
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
                    description = "Меню треда",
                    enabled = content != null,
                    onClick = { tools.menu = true },
                )
                ThreadOverflowMenu(tools, content, actions)
            }
        }
    }
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
) {
    ThreadToolbarSlot {
        ThreadToolbarButton(UiIconKind.BACK, "Назад", onClick = actions.onBack)
    }
    ThreadToolbarSlot {
        ThreadToolbarButton(
            kind = UiIconKind.BOOKMARK,
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
        description = "Галерея",
        enabled = content != null,
        onClick = { tools.dialog = ThreadDialog.GALLERY },
    )
}
