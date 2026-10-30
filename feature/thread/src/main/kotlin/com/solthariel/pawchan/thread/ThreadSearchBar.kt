package com.solthariel.pawchan.thread

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.core.ui.UiTopAppBar

@Composable
internal fun ThreadSearchBar(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    onResult: (Int) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val closeSearch = {
        focusManager.clearFocus()
        keyboard?.hide()
        tools.closeSearch()
    }
    BackHandler(onBack = closeSearch)
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    UiTopAppBar(
        title = { ThreadSearchField(tools, content, focusRequester) },
        navigationIcon = { ThreadToolbarButton(UiIconKind.BACK, "Закрыть поиск", onClick = closeSearch) },
        actions = { ThreadSearchNavigation(tools, onResult) },
    )
}

@Composable
private fun ThreadSearchField(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    focusRequester: FocusRequester,
) {
    UiToolbarSearchField(
        tools.query,
        { tools.search(content, it) },
        focusRequester,
        Modifier.fillMaxWidth(),
        tools.flag?.title ?: "Поиск в треде",
    )
}

@Composable
private fun RowScope.ThreadSearchNavigation(
    tools: ThreadToolsState,
    onResult: (Int) -> Unit,
) {
    Text(
        text = if (tools.results.isEmpty()) "0" else "${tools.resultIndex + 1}/${tools.results.size}",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        maxLines = 1,
    )
    ThreadToolbarButton(
        kind = UiIconKind.UP,
        description = "Предыдущее совпадение",
        enabled = tools.results.isNotEmpty(),
        onClick = { onResult(-1) },
    )
    ThreadToolbarButton(
        kind = UiIconKind.DOWN,
        description = "Следующее совпадение",
        enabled = tools.results.isNotEmpty(),
        onClick = { onResult(1) },
    )
}
