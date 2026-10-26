package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterFields
import com.solthariel.pawchan.domain.filter.ContentFilterMode

@Composable
internal fun FilterEditorFields(draft: ContentFilterDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterLineInput("Доски", draft.boards, { draft.boards = it }, Modifier.weight(1f), singleLine = true)
            FilterLineInput("Треды", draft.threadIds, { draft.threadIds = it }, Modifier.weight(1f), singleLine = true)
        }
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterCheck("ОП-посты", draft.threads, { draft.threads = it }, Modifier.weight(1f))
                FilterCheck("Ответы", draft.comments, { draft.comments = it }, Modifier.weight(1f))
            }
            if (draft.threads) FilterOpFields(draft)
        }
        FilterMatchModes(draft)
        FilterLineInput(
            "Условие",
            draft.text,
            { draft.text = it },
            presentation = filterConditionPresentation(draft.mode),
        )
        if (draft.mode == ContentFilterMode.REGEX.name) FilterRegexCopyButton(draft.text)
    }
}

@Composable
private fun FilterMatchModes(draft: ContentFilterDraft) {
    val basic = listOf(ContentFilterMode.EXACT_PHRASE, ContentFilterMode.REGEX)
    val selectedMode = ContentFilterMode.valueOf(draft.mode)
    val modes = if (selectedMode in basic) basic else basic + selectedMode
    Column {
        if (selectedMode !in basic) {
            Text(
                "Старое условие сохранено. Для смены способа поиска выберите режим ниже.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            basic.forEach { mode -> FilterModeChoice(draft, mode, Modifier.weight(1f)) }
        }
        modes.filterNot { it in basic }.forEach { mode -> FilterModeChoice(draft, mode, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun FilterModeChoice(
    draft: ContentFilterDraft,
    mode: ContentFilterMode,
    modifier: Modifier,
) {
    Row(
        modifier.heightIn(min = 48.dp).selectable(
            selected = draft.mode == mode.name,
            role = Role.RadioButton,
            onClick = { draft.selectMode(mode) },
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = draft.mode == mode.name, onClick = null, modifier = Modifier.size(24.dp))
        Text(filterModeLabel(mode), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun FilterOpFields(draft: ContentFilterDraft) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val subject = draft.fields != ContentFilterFields.ORIGINAL_POST.name
        val body = draft.fields != ContentFilterFields.SUBJECT.name
        FilterCheck("Заголовок", subject, { enabled ->
            draft.fields = if (enabled) ContentFilterFields.EITHER.name else ContentFilterFields.ORIGINAL_POST.name
        }, Modifier.weight(1f))
        FilterCheck("Текст", body, { enabled ->
            draft.fields = if (enabled) ContentFilterFields.EITHER.name else ContentFilterFields.SUBJECT.name
        }, Modifier.weight(1f))
    }
}

@Composable
internal fun FilterCheck(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.heightIn(min = 48.dp).toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null, modifier = Modifier.size(24.dp))
        Text(label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
