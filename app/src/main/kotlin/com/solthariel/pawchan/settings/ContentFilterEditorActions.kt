package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R

@Composable
internal fun FilterEditorActions(
    draft: ContentFilterDraft,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (compactFilterActions(maxWidth, onDelete != null)) {
            CompactFilterActions(draft, onClose, onDelete, confirm)
        } else {
            ExpandedFilterActions(draft, onClose, onDelete, confirm)
        }
    }
}

@Composable
private fun compactFilterActions(
    width: Dp,
    hasDelete: Boolean,
): Boolean {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val toggle = measurer.measure(AnnotatedString("Включено"), MaterialTheme.typography.bodyLarge).size.width
    val cancel = measurer.measure(AnnotatedString("Отмена"), MaterialTheme.typography.labelLarge).size.width
    val save = measurer.measure(AnnotatedString("Сохранить"), MaterialTheme.typography.labelLarge).size.width
    return with(density) { filterEditorActionsFit(width, toggle.toDp(), cancel.toDp(), save.toDp(), hasDelete) }
}

@Composable
private fun CompactFilterActions(
    draft: ContentFilterDraft,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterCheck(
            "Включено",
            draft.enabled,
            { draft.enabled = it },
            Modifier.weight(1f).testTag("filter-editor-enabled"),
        )
        FilterDeleteAction(onDelete)
        TextButton(onClick = onClose) { Text("Отмена") }
        confirm()
    }
}

@Composable
private fun ExpandedFilterActions(
    draft: ContentFilterDraft,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
) {
    Column {
        FilterCheck(
            "Правило включено",
            draft.enabled,
            { draft.enabled = it },
            Modifier.fillMaxWidth().testTag("filter-editor-enabled"),
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FilterDeleteAction(onDelete)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onClose) { Text("Отмена") }
            confirm()
        }
    }
}

@Composable
private fun FilterDeleteAction(onDelete: (() -> Unit)?) {
    if (onDelete != null) {
        IconButton(onClick = onDelete) {
            Icon(
                painterResource(R.drawable.ic_filter_delete),
                contentDescription = "Удалить правило",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

internal fun filterEditorActionsFit(
    available: Dp,
    toggleText: Dp,
    cancelText: Dp,
    saveText: Dp,
    hasDelete: Boolean,
): Boolean {
    val toggle = 24.dp + 8.dp + toggleText
    val buttons = maxOf(48.dp, cancelText + 24.dp) + maxOf(48.dp, saveText + 24.dp)
    val delete = if (hasDelete) 48.dp + 4.dp else 0.dp
    return toggle + buttons + delete + 8.dp <= available
}
