package com.solthariel.pawchan.boards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R.string as L

internal val CATEGORY_CHIPS_HEIGHT = 48.dp

@Composable
internal fun BoardsCategoryChips(
    categories: List<BoardCategoryUi>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    val res = LocalResources.current

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = CATEGORY_CHIPS_HEIGHT)
                .padding(horizontal = BOARD_ROW_INSET)
                .testTag("board-category-chips")
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryChip(res.getString(L.ui_all), selected.isEmpty()) { onSelect("") }
        categories.forEach { category ->
            CategoryChip(category.title, selected == category.title) { onSelect(category.title) }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        border = BorderStroke(1.dp, borderColor),
        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.surface),
    )
}
