package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterSample
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ContentFilterEditor(
    rule: ContentFilterRule,
    editorKey: String,
    onClose: () -> Unit,
    onSave: (ContentFilterRule) -> Unit,
    samples: List<ContentFilterSample> = emptyList(),
    onDelete: (() -> Unit)? = null,
) {
    val draft = rememberSaveable(editorKey, saver = ContentFilterDraft.Saver) { ContentFilterDraft(rule) }
    val next = draft.rule(rule)
    var checked by remember(editorKey) { mutableStateOf<ContentFilterRule?>(null) }
    var validation by remember(editorKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(next) {
        validation = withContext(Dispatchers.Default) { contentFilterValidationError(next) }
        checked = next
    }
    val error = draft.inputError() ?: validation
    FilterEditorWindow(
        onClose = onClose,
        onDelete = onDelete,
        confirm = {
            TextButton(enabled = checked == next && error == null, onClick = { onSave(next) }) {
                Text("Сохранить")
            }
        },
    ) {
        FilterEditorFields(draft)
        ContentFilterProbe(next, samples)
        FilterLegacyFields(draft)
        if (error != null && !filterEmptyErrorIsInline(error, draft.text)) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FilterEditorWindow(
    onClose: () -> Unit,
    onDelete: (() -> Unit)?,
    confirm: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    AppDialog(onClose) {
        Column {
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(AppDialogStyle.contentPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Удалить") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("Отмена") }
                confirm()
            }
        }
    }
}
