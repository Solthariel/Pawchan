package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterLineInput("Доски", draft.boards, { draft.boards = it }, Modifier.weight(1f), singleLine = true)
            FilterLineInput("Треды", draft.threadIds, { draft.threadIds = it }, Modifier.weight(1f), singleLine = true)
        }
        FilterOptionGroup(
            "Где скрывать",
            FilterPostScope.entries.map { it.name to it.label },
            draft.postScope,
        ) { draft.postScope = it }
        FilterOptionGroup(
            "Как искать",
            FILTER_EDITOR_MODES.map { it.name to filterModeLabel(it) },
            draft.mode,
        ) { draft.selectMode(ContentFilterMode.valueOf(it)) }
        Text(filterModeHint(draft.mode), style = MaterialTheme.typography.bodySmall)
        FilterLineInput("Что искать", draft.text, { draft.text = it })
        FilterOptionGroup(
            "Совпадение",
            listOf(FilterBoundary.WORD.name to "Слово целиком", FilterBoundary.PART.name to "Часть слова"),
            draft.boundary,
        ) { draft.boundary = it }
        Text(filterBoundaryHint(draft.boundary), style = MaterialTheme.typography.bodySmall)
        FilterLineInput("Не скрывать, если есть…", draft.exceptions, { draft.exceptions = it })
        Text(
            "Исключения разделяйте запятыми или переносами строк. Любое совпадение оставит пост видимым. " +
                "Выбранный способ совпадения действует и на исключения.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

internal val FILTER_EDITOR_MODES =
    listOf(ContentFilterMode.PHRASE, ContentFilterMode.ANY_TERM, ContentFilterMode.ALL_TERMS)

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
