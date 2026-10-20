package com.solthariel.pawchan.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle

@Composable
internal fun FilterEditorWindow(
    draft: ContentFilterDraft,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AppDialog(onClose) { FilterEditorBody(draft, onClose, onDelete, confirm, content) }
}

@Composable
internal fun FilterEditorBody(
    draft: ContentFilterDraft,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val scroll = rememberScrollState()
    val outline = MaterialTheme.colorScheme.outline
    Column {
        Column(
            Modifier
                .weight(1f, fill = false)
                .testTag("filter-editor-scroll")
                .filterEditorScrollbar(scroll, outline)
                .verticalScroll(scroll)
                .padding(AppDialogStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
        HorizontalDivider()
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            FilterCheck("Правило включено", draft.enabled, { draft.enabled = it })
            FilterEditorButtons(onClose, onDelete, confirm)
        }
    }
}

@Composable
private fun FilterEditorButtons(
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onDelete != null) TextButton(onClick = onDelete) { Text("Удалить") }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onClose) { Text("Отмена") }
        confirm()
    }
}

private fun Modifier.filterEditorScrollbar(
    scroll: ScrollState,
    color: Color,
): Modifier =
    drawWithContent {
        drawContent()
        if (scroll.maxValue > 0) {
            val track = size.height
            val thumb = (track * track / (track + scroll.maxValue)).coerceAtLeast(16.dp.toPx()).coerceAtMost(track)
            val top = (track - thumb) * scroll.value / scroll.maxValue
            drawRoundRect(
                color.copy(alpha = 0.7f),
                Offset(size.width - 4.dp.toPx(), top),
                Size(2.dp.toPx(), thumb),
                CornerRadius(1.dp.toPx()),
            )
        }
    }
