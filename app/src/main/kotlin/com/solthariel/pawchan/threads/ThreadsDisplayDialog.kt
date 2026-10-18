package com.solthariel.pawchan.threads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.domain.settings.MAX_VISIBLE
import com.solthariel.pawchan.domain.settings.MIN_VISIBLE
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import kotlin.math.roundToInt

/** Limit affects only the collapsed tail, never the available records. */
@Composable
internal fun ThreadsDisplayDialog(
    limits: NavigationPreferences,
    onConfirm: (NavigationPreferences) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(limits) { mutableStateOf(limits.normalized()) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отображение списков") },
        text = {
            Column {
                VisibleCountRow("Страницы", draft.pages) { draft = draft.copy(pages = it) }
                VisibleCountRow("Избранные треды", draft.favorites) { draft = draft.copy(favorites = it) }
                VisibleCountRow("Ответы", draft.replies) { draft = draft.copy(replies = it) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(draft)
                onDismiss()
            }) { Text("Применить") }
        },
    )
}

@Composable
private fun VisibleCountRow(
    title: String,
    value: Int,
    onChange: (Int) -> Unit,
) {
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
            modifier = Modifier.semantics { contentDescription = "$title: количество" },
        )
    }
}
