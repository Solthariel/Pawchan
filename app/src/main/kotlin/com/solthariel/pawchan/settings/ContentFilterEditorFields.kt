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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.FilterBoundary
import com.solthariel.pawchan.R.string as L

@Composable
internal fun FilterEditorFields(draft: ContentFilterDraft) {
    val res = LocalResources.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterLineInput(res.getString(L.ui_boards), draft.boards, {
                draft.boards = it
            }, Modifier.weight(1f), singleLine = true)
            FilterLineInput(res.getString(L.ui_threads), draft.threadIds, {
                draft.threadIds = it
            }, Modifier.weight(1f), singleLine = true)
        }
        FilterOptionGroup(
            res.getString(L.ui_where_to_hide),
            FilterPostScope.entries.map { it.name to it.label },
            draft.postScope,
        ) { draft.postScope = it }
        FilterOptionGroup(
            res.getString(L.ui_how_to_search),
            FILTER_EDITOR_MODES.map { it.name to filterModeLabel(it) },
            draft.mode,
        ) { draft.selectMode(ContentFilterMode.valueOf(it)) }
        Text(filterModeHint(draft.mode), style = MaterialTheme.typography.bodySmall)
        FilterLineInput(res.getString(L.ui_what_to_search), draft.text, { draft.text = it })
        FilterOptionGroup(
            res.getString(L.ui_match),
            listOf(
                FilterBoundary.WORD.name to res.getString(L.ui_whole_word),
                FilterBoundary.PART.name to res.getString(L.ui_part_of_word),
            ),
            draft.boundary,
        ) { draft.boundary = it }
        Text(filterBoundaryHint(draft.boundary), style = MaterialTheme.typography.bodySmall)
        FilterLineInput(res.getString(L.ui_do_not_hide_if_there), draft.exceptions, { draft.exceptions = it })
        Text(
            res.getString(L.ui_separate_exceptions_with_commas_or),
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
