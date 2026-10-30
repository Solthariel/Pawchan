package com.solthariel.pawchan.thread

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiTopAppBar
import com.solthariel.pawchan.feature.thread.R

@Composable
internal fun ThreadSelectionBar(
    tools: ThreadToolsState,
    actions: ThreadSelectionActions,
) {
    if (!tools.selecting) return
    UiTopAppBar(
        title = { Text("Выбрано: ${tools.selected.size}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            ThreadToolbarButton(UiIconKind.BACK, "Отменить выделение", onClick = tools::clearSelection)
        },
        actions = {
            SelectionActionIcon(
                R.drawable.ic_thread_screenshot,
                "Скриншот",
                tools.selected.isNotEmpty() && !tools.exporting,
                actions.screenshot,
            )
            SelectionActionIcon(R.drawable.ic_thread_quote, "Цитировать", tools.selected.isNotEmpty(), actions.quote)
            SelectionActionIcon(
                R.drawable.ic_thread_report,
                "Жалоба",
                tools.selected.isNotEmpty() && actions.report != null,
                { actions.report?.invoke() },
            )
        },
    )
}

@Composable
private fun SelectionActionIcon(
    drawable: Int,
    description: String,
    enabled: Boolean,
    action: () -> Unit,
) {
    IconButton(onClick = action, enabled = enabled) {
        Icon(painterResource(drawable), description)
    }
}
