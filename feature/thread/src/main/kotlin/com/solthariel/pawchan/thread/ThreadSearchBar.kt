package com.solthariel.pawchan.thread

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

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
    Row(
        Modifier
            .fillMaxWidth()
            .height(APP_TOOLBAR_BUTTON_SIZE)
            .padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadToolbarButton(UiIconKind.BACK, "Закрыть поиск", onClick = closeSearch)
        ThreadSearchField(tools, content, focusRequester)
        ThreadSearchNavigation(tools, onResult)
    }
}

@Composable
private fun RowScope.ThreadSearchField(
    tools: ThreadToolsState,
    content: ThreadUiState.Content?,
    focusRequester: FocusRequester,
) {
    Row(
        modifier =
            Modifier
                .weight(1f)
                .height(34.dp)
                .padding(horizontal = 4.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                    RoundedCornerShape(17.dp),
                ).padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiIcon(
            UiIconKind.SEARCH,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        BasicTextField(
            value = tools.query,
            onValueChange = { tools.search(content, it) },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner -> ThreadSearchPlaceholder(tools, inner) },
        )
        if (tools.query.isNotEmpty()) {
            ThreadToolbarButton(UiIconKind.CLOSE, "Очистить поиск") {
                tools.search(content, "")
            }
        }
    }
}

@Composable
private fun ThreadSearchPlaceholder(
    tools: ThreadToolsState,
    inner: @Composable () -> Unit,
) {
    if (tools.query.isEmpty()) {
        Text(
            tools.flag?.title ?: "Поиск в треде",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
    inner()
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
