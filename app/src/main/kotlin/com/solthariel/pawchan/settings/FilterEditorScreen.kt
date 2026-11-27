package com.solthariel.pawchan.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.R.string as L

/** Включение правила переключается в списке правил; справка доступна из меню списка. */
@Composable
internal fun FilterEditorScreen(
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        FilterEditorHeader(onClose, onDelete, confirm)
        Column(
            Modifier
                .weight(1f)
                .testTag("filter-editor-full-scroll")
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun FilterEditorHeader(
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
) {
    val res = LocalResources.current

    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiToolbarButton(UiIconKind.CLOSE, res.getString(L.ui_close_rule), onClick = onClose)
        Text(
            res.getString(L.ui_auto_hide_rule),
            Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (onDelete != null) {
            UiToolbarButton(UiIconKind.DELETE, res.getString(L.ui_delete_rule), onClick = onDelete)
        }
        confirm()
    }
}
