package com.solthariel.pawchan.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun FilterEditorWindow(
    draft: ContentFilterDraft,
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        FilterEditorViewport(onClose) { FilterEditorBody(draft, onClose, onDelete, confirm, content) }
    }
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) { content() }
        HorizontalDivider()
        FilterEditorActions(draft, onClose, onDelete, confirm)
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
