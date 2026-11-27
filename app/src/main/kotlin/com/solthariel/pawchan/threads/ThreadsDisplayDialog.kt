package com.solthariel.pawchan.threads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.settings.MAX_VISIBLE
import com.solthariel.pawchan.domain.settings.MIN_VISIBLE
import com.solthariel.pawchan.domain.settings.NavigationLayout
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.settings.ReorderRow
import com.solthariel.pawchan.settings.moved
import kotlin.math.roundToInt
import com.solthariel.pawchan.R.string as L

/** Limit affects only the collapsed tail, never the available records. */
@Composable
internal fun ThreadsDisplayDialog(
    limits: NavigationPreferences,
    onConfirm: (NavigationPreferences) -> Unit,
    onDismiss: () -> Unit,
) {
    val res = LocalResources.current

    var draft by remember(limits) { mutableStateOf(limits.normalized()) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(res.getString(L.ui_list_display)) },
        text = {
            Column {
                ThreadsOrderRows(draft.layout) { draft = draft.copy(layout = it) }
                VisibleCountRow(res.getString(L.ui_pages), draft.pages) { draft = draft.copy(pages = it) }
                VisibleCountRow(res.getString(L.ui_favorite_threads), draft.favorites) {
                    draft =
                        draft.copy(favorites = it)
                }
                VisibleCountRow(res.getString(L.ui_replies), draft.replies) { draft = draft.copy(replies = it) }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { draft = draft.withThreadsDefaults() }) { Text(res.getString(L.ui_reset)) }
                TextButton(onClick = onDismiss) { Text(res.getString(L.ui_cancel)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(draft)
                onDismiss()
            }) { Text(res.getString(L.ui_apply)) }
        },
    )
}

/** Порядок разделов ↑↓; у ссылок глаз: на экране или в ⋮. */
@Composable
private fun ThreadsOrderRows(
    layout: NavigationLayout,
    onChange: (NavigationLayout) -> Unit,
) {
    val res = LocalResources.current
    val order = layout.threads
    Column(Modifier.testTag("threads-order").padding(bottom = 8.dp)) {
        order.forEachIndexed { index, item ->
            val title = res.threadsHubLabel(item)
            ReorderRow(
                title,
                index,
                order.size,
                { delta -> onChange(layout.copy(threads = order.moved(index, delta))) },
                trailing = {
                    if (item.movableToMenu) {
                        ScreenToggle(title, item !in layout.threadsInMenu) { onScreen ->
                            val menu = if (onScreen) layout.threadsInMenu - item else layout.threadsInMenu + item
                            onChange(layout.copy(threadsInMenu = menu))
                        }
                    }
                },
            )
        }
        Text(
            res.getString(L.ui_hidden_in_menu_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScreenToggle(
    title: String,
    onScreen: Boolean,
    onChange: (Boolean) -> Unit,
) {
    val description = LocalResources.current.getString(L.ui_show_on_screen_named, title)
    IconToggleButton(
        checked = onScreen,
        onCheckedChange = onChange,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        UiIcon(
            if (onScreen) UiIconKind.SHOW else UiIconKind.HIDE,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Modifier.size(24.dp),
        )
    }
}

@Composable
private fun VisibleCountRow(
    title: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
    val res = LocalResources.current

    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(MIN_VISIBLE, MAX_VISIBLE)) },
            valueRange = MIN_VISIBLE.toFloat()..MAX_VISIBLE.toFloat(),
            steps = MAX_VISIBLE - MIN_VISIBLE - 1,
            modifier = Modifier.semantics { contentDescription = res.getString(L.ui_count, title) },
        )
    }
}
