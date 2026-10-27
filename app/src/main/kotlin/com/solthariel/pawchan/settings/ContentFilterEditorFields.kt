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
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.FilterBoundary

@Composable
internal fun FilterEditorFields(draft: ContentFilterDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterLineInput("Доски", draft.boards, { draft.boards = it }, Modifier.weight(1f), singleLine = true)
            FilterLineInput("Треды", draft.threadIds, { draft.threadIds = it }, Modifier.weight(1f), singleLine = true)
        }
        Text("Как искать", style = MaterialTheme.typography.labelLarge)
        Column(Modifier.selectableGroup()) {
            listOf(ContentFilterMode.PHRASE, ContentFilterMode.ANY_TERM, ContentFilterMode.ALL_TERMS).forEach { mode ->
                FilterChoice(filterModeLabel(mode), draft.mode == mode.name) { draft.selectMode(mode) }
            }
        }
        FilterLineInput("Что искать", draft.text, { draft.text = it })
        Text(
            "Для «Любое» и «Все» — одно слово или фраза на строку. Пробел не разделяет значения.",
            style = MaterialTheme.typography.bodySmall,
        )
        FilterBoundaryChoices(draft)
        FilterCheck("Учитывать регистр", draft.caseSensitive, { draft.caseSensitive = it })
        FilterLineInput("Не скрывать, если есть…", draft.exceptions, { draft.exceptions = it })
        Text(
            "Исключения — по одному на строку. Любое исключение в заголовке или тексте оставляет пост видимым.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun FilterBoundaryChoices(draft: ContentFilterDraft) {
    Text("Границы", style = MaterialTheme.typography.labelLarge)
    Column(Modifier.selectableGroup()) {
        FilterBoundary.entries.forEach { boundary ->
            val label =
                when (boundary) {
                    FilterBoundary.WORD -> "Слово целиком"
                    FilterBoundary.PREFIX -> "Начало слова"
                    FilterBoundary.PART -> "Любая часть"
                }
            FilterChoice(label, draft.boundary == boundary.name) { draft.boundary = boundary.name }
        }
    }
}

@Composable
private fun FilterChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected, onClick = null, modifier = Modifier.size(24.dp))
        Text(label, Modifier.padding(start = 8.dp))
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
