package com.solthariel.pawchan.thread

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.MenuNavigation
import com.solthariel.pawchan.core.ui.OVAL_BUTTON_SHAPE
import com.solthariel.pawchan.core.ui.UiBackwardIndicator
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiMenuIcon
import com.solthariel.pawchan.core.ui.UiSubmenuIndicator
import kotlinx.coroutines.CoroutineScope
import com.solthariel.pawchan.core.ui.ModalDropdownMenu as DropdownMenu
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun ThreadOverflowMenu(
    tools: ThreadToolsState,
    content: ThreadUiState.Content? = null,
    actions: ThreadActions? = null,
    compact: Boolean = false,
) {
    val scope = rememberCoroutineScope()
    DropdownMenu(expanded = tools.menu, onDismissRequest = { tools.closeMenu() }) {
        when {
            tools.advanced -> {
                ThreadSearchFlagItems(tools)
            }

            tools.contents -> {
                ThreadContentsItems(tools, content, actions)
            }

            else -> {
                if (compact) ThreadCompactMenuItems(tools, content, actions)
                ThreadMainMenuItems(tools, content, actions, scope)
            }
        }
    }
}

@Composable
private fun ThreadMainMenuItems(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    actions: ThreadActions?,
    scope: CoroutineScope,
) {
    val res = LocalResources.current

    if (tools.readingReturn != null) {
        ThreadMenuItem(res.getString(L.ui_return_to_reading_position), icon = UiIconKind.HISTORY) {
            tools.closeMenu()
            tools.onReadingReturn()
        }
    }
    ThreadMenuItem(res.getString(L.ui_advanced_search), icon = UiIconKind.SEARCH) { tools.advanced = true }
    ThreadMenuItem(res.getString(L.ui_select), icon = UiIconKind.SELECT_ALL) {
        tools.menu = false
        tools.selecting = true
    }
    ThreadMenuItem(
        res.getString(L.ui_content),
        icon = UiIconKind.DOCUMENT,
        navigation = MenuNavigation.FORWARD,
    ) { tools.contents = true }
    ThreadMenuItem(
        res.getString(L.ui_appearance_0983),
        icon = UiIconKind.PALETTE,
    ) { tools.openDialog(ThreadDialog.APPEARANCE) }
    HorizontalDivider()
    ThreadMenuItem(
        res.getString(L.ui_save_copy_cac8),
        icon = UiIconKind.SAVE,
        enabled = !tools.exporting,
    ) { tools.openDialog(ThreadDialog.ARCHIVE) }
    content?.address?.let { ThreadArhivachItems(it, tools, actions?.history, scope) }
    content?.let {
        HorizontalDivider()
        ThreadMenuStatistics(it)
    }
}

@Composable
private fun ThreadSearchFlagItems(tools: ThreadToolsState) {
    val res = LocalResources.current

    ThreadMenuItem(res.getString(L.ui_back), navigation = MenuNavigation.BACK) { tools.advanced = false }
    ThreadSearchFlag.entries.forEach { flag ->
        ThreadMenuItem(flag.title) { tools.selectFlag(flag) }
    }
}

/** «Содержимое ›»: перезагрузка, стирание и очистка истории треда, скрытые посты. */
@Composable
private fun ThreadContentsItems(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    actions: ThreadActions?,
) {
    val res = LocalResources.current

    val history = actions?.history
    ThreadMenuItem(res.getString(L.ui_back), navigation = MenuNavigation.BACK) { tools.contents = false }
    ThreadMenuItem(res.getString(L.ui_reload), icon = UiIconKind.REFRESH) {
        tools.closeMenu()
        actions?.onRefresh?.invoke()
    }
    if (history != null) {
        ThreadMenuItem(res.getString(L.ui_erase), icon = UiIconKind.DELETE) {
            tools.closeMenu()
            history.erase()
        }
        if (content?.hasOldDeletedPosts() == true) {
            ThreadMenuItem(res.getString(L.ui_clear_old), icon = UiIconKind.DELETE) {
                tools.closeMenu()
                history.clearOld()
            }
        }
        ThreadMenuItem(
            res.getString(L.ui_clear_deleted),
            icon = UiIconKind.DELETE,
            enabled = content?.posts?.any { it.metadata.changes.deleted } == true,
        ) {
            tools.closeMenu()
            history.clearDeleted()
        }
    }
    ThreadMenuItem(
        res.getString(L.ui_hidden_posts),
        icon = UiIconKind.HIDE,
    ) { tools.openDialog(ThreadDialog.HIDDEN_RULES) }
}

@Composable
fun ThreadMenuItem(
    title: String,
    enabled: Boolean = true,
    navigation: MenuNavigation? = null,
    icon: UiIconKind? = null,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
        text = { Text(title) },
        leadingIcon =
            if (navigation == MenuNavigation.BACK) {
                { UiBackwardIndicator() }
            } else if (icon != null) {
                { UiMenuIcon(icon) }
            } else {
                null
            },
        trailingIcon =
            if (navigation == MenuNavigation.FORWARD) {
                { UiSubmenuIndicator() }
            } else {
                null
            },
        enabled = enabled,
        onClick = onClick,
    )
}

internal fun ThreadToolsState.closeMenu() {
    menu = false
    advanced = false
    contents = false
}

internal fun ThreadToolsState.openDialog(value: ThreadDialog) {
    closeMenu()
    dialog = value
}

/** Цикличный тред, у которого пост сразу после ОП-поста уже удалён: сервер отбросил старые посты. */
internal fun ThreadUiState.Content.hasOldDeletedPosts(): Boolean =
    flags.endless && posts
        .getOrNull(1)
        ?.metadata
        ?.changes
        ?.deleted == true

internal const val ARCHIVE_REQUEST = "archive-request"
