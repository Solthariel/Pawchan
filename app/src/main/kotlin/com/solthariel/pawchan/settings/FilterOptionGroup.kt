package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiChoiceSegments

/** Equal Material segments stay in one row; labels wrap without reducing the user's font. */
@Composable
internal fun FilterOptionGroup(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().selectableGroup().testTag("filter-options-$title"),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        UiChoiceSegments(
            options.map { it.second },
            options.indexOfFirst { it.first == selected },
            true,
            wrapLabels = true,
        ) {
            onSelect(options[it].first)
        }
    }
}
