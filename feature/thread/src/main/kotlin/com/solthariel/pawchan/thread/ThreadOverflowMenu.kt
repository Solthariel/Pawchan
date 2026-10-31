package com.solthariel.pawchan.thread

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.solthariel.pawchan.core.ui.MenuNavigation
import com.solthariel.pawchan.core.ui.OVAL_BUTTON_SHAPE
import com.solthariel.pawchan.core.ui.UiBackwardIndicator
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun ThreadOverflowMenu(
    tools: ThreadToolsState,
    content: ThreadUiState.Content? = null,
    actions: ThreadActions? = null,
) {
    val scope = rememberCoroutineScope()
    DropdownMenu(expanded = tools.menu, onDismissRequest = { tools.closeMenu() }) {
        when {
            tools.advanced -> ThreadSearchFlagItems(tools)
            tools.contents -> ThreadContentsItems(tools, content, actions)
            else -> ThreadMainMenuItems(tools, content, actions, scope)
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
    if (tools.readingReturn != null) {
        ThreadMenuItem("Вернуться к месту чтения") {
            tools.closeMenu()
            tools.onReadingReturn()
        }
    }
    ThreadMenuItem("Расширенный поиск") { tools.advanced = true }
    ThreadMenuItem("Выделить") {
        tools.menu = false
        tools.selecting = true
    }
    ThreadMenuItem("Содержимое", navigation = MenuNavigation.FORWARD) { tools.contents = true }
    ThreadMenuItem("Внешний вид") { tools.openDialog(ThreadDialog.APPEARANCE) }
    HorizontalDivider()
    ThreadMenuItem("Сохранить копию…", enabled = !tools.exporting) { tools.openDialog(ThreadDialog.ARCHIVE) }
    content?.address?.let { ThreadArhivachItems(it, tools, actions?.history, scope) }
    content?.let {
        HorizontalDivider()
        ThreadMenuStatistics(it)
    }
}

@Composable
private fun ThreadSearchFlagItems(tools: ThreadToolsState) {
    ThreadSearchFlag.entries.forEach { flag ->
        ThreadMenuItem(flag.title) {
            tools.flag = flag
            tools.searchOpen = true
            tools.closeMenu()
        }
    }
}

/** «Содержимое ›»: перезагрузка, стирание и очистка истории треда, скрытые посты. */
@Composable
private fun ThreadContentsItems(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    actions: ThreadActions?,
) {
    val history = actions?.history
    ThreadMenuItem("Назад", navigation = MenuNavigation.BACK) { tools.contents = false }
    ThreadMenuItem("Перезагрузить") {
        tools.closeMenu()
        actions?.onRefresh?.invoke()
    }
    if (history != null) {
        ThreadMenuItem("Стереть") {
            tools.closeMenu()
            history.erase()
        }
        if (content?.hasOldDeletedPosts() == true) {
            ThreadMenuItem("Очистить старые") {
                tools.closeMenu()
                history.clearOld()
            }
        }
        ThreadMenuItem("Очистить удалённые", enabled = content?.posts?.any { it.metadata.changes.deleted } == true) {
            tools.closeMenu()
            history.clearDeleted()
        }
    }
    ThreadMenuItem("Скрытые посты") { tools.openDialog(ThreadDialog.HIDDEN_RULES) }
}

@Composable
internal fun ThreadMenuItem(
    title: String,
    enabled: Boolean = true,
    navigation: MenuNavigation? = null,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier.clip(OVAL_BUTTON_SHAPE),
        text = { Text(title) },
        leadingIcon =
            if (navigation == MenuNavigation.BACK) {
                { UiBackwardIndicator() }
            } else {
                null
            },
        trailingIcon =
            if (navigation == MenuNavigation.FORWARD) {
                { UiForwardIndicator() }
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
