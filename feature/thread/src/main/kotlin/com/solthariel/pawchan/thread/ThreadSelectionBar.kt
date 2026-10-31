package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.thread.R

@Composable
internal fun ThreadSelectionBar(
    tools: ThreadToolsState,
    actions: ThreadSelectionActions,
) {
    if (!tools.selecting) return
    Row(
        Modifier.fillMaxWidth().height(APP_TOP_BAR_HEIGHT).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadToolbarButton(UiIconKind.BACK, "Отменить выделение", onClick = tools::clearSelection)
        Text(
            "Выбрано: ${tools.selected.size}",
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
    }
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
