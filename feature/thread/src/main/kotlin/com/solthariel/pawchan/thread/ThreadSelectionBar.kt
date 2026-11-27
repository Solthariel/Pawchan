package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.thread.R
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun ThreadSelectionBar(
    tools: ThreadToolsState,
    actions: ThreadSelectionActions,
) {
    val res = LocalResources.current

    if (!tools.selecting) return
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = APP_TOP_BAR_HEIGHT,
            ).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThreadToolbarButton(UiIconKind.BACK, res.getString(L.ui_clear_selection), onClick = tools::clearSelection)
        Text(
            res.getString(L.ui_selected, tools.selected.size),
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            fontSize = com.solthariel.pawchan.core.ui.APP_TOOLBAR_TITLE_SIZE,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SelectionActionIcon(
            R.drawable.ic_thread_screenshot,
            res.getString(L.ui_screenshot),
            tools.shots.ready && !tools.exporting,
            actions.screenshot,
        )
        SelectionActionIcon(
            R.drawable.ic_thread_quote,
            res.getString(L.ui_quote),
            tools.selected.isNotEmpty() && actions.quote != null,
            { actions.quote?.invoke() },
        )
        SelectionActionIcon(
            R.drawable.ic_thread_report,
            res.getString(L.ui_report),
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
