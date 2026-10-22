package com.solthariel.pawchan.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.catalog.CATALOG_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.FAST_SCROLL_CONTENT_INSET
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.UiDisclosureIndicator

@Composable
internal fun BoardsDirectoryList(
    content: BoardsListUi,
    onExpandedChange: (String, Boolean) -> Unit,
    actions: BoardListActions,
    modifier: Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Box(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = BOARD_LIST_START,
                    top = 0.dp,
                    end = FAST_SCROLL_CONTENT_INSET,
                    bottom = CATALOG_BOTTOM_BAR_HEIGHT + BOARDS_BOTTOM_CONTENT_GAP,
                ),
        ) {
            items(content.rows, key = { it.key }, contentType = { it.contentType }) { row ->
                BoardListSlot(row, content.dimensions, onExpandedChange, actions)
            }
        }
        PawchanFastScrollbar(
            state = listState,
            modifier = Modifier.align(Alignment.CenterEnd),
            itemSizes = content.itemSizes,
        )
    }
}

@Composable
private fun BoardListSlot(
    row: BoardListItem,
    dimensions: BoardListDimensions,
    onExpandedChange: (String, Boolean) -> Unit,
    actions: BoardListActions,
) {
    val leadingGap =
        when (row) {
            is BoardListItem.Header -> if (row.leadingGap) BOARD_SECTION_GAP else 0.dp
            is BoardListItem.Board -> if (row.position.first) BOARD_GROUP_GAP else 0.dp
            BoardListItem.Empty -> 0.dp
        }
    Box(modifier = Modifier.fillMaxWidth().height(dimensions.height(row)).padding(top = leadingGap)) {
        when (row) {
            is BoardListItem.Header -> {
                BoardCategoryHeader(row, onExpandedChange)
            }

            is BoardListItem.Board -> {
                BoardDirectoryRow(row, actions)
            }

            BoardListItem.Empty -> {
                Text(
                    text = stringResource(R.string.boards_search_empty),
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BoardCategoryHeader(
    row: BoardListItem.Header,
    onExpandedChange: (String, Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .clickable(enabled = row.enabled) { onExpandedChange(row.categoryKey, !row.expanded) }
                .padding(horizontal = BOARD_ROW_INSET, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(20.dp)) {
            if (row.enabled || row.expanded) UiDisclosureIndicator(row.expanded)
        }
        Text(
            text = row.label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color =
                if (row.enabled || row.expanded) {
                    MaterialTheme.colorScheme.onBackground
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            fontWeight = FontWeight.SemiBold,
        )
    }
}
