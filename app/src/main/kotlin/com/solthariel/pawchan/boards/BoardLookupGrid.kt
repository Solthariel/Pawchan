package com.solthariel.pawchan.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.BoardAddress

@Composable
internal fun BoardLookupGrid(
    boards: List<BoardLookupBoard>,
    columns: Int,
    rows: Int,
    select: (BoardAddress) -> Unit,
    tag: String,
) {
    if (rows == 0) return
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxWidth().height(BOARD_LOOKUP_ROW_HEIGHT * rows).testTag(tag),
    ) {
        items(boards, key = { it.key }) { board -> BoardLookupCode(board, select) }
    }
}

@Composable
private fun BoardLookupCode(
    board: BoardLookupBoard,
    select: (BoardAddress) -> Unit,
) {
    val background = if (board.selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    Box(
        Modifier
            .fillMaxWidth()
            .height(BOARD_LOOKUP_ROW_HEIGHT)
            .semantics {
                contentDescription = board.description
                selected = board.selected
            }.clickable { select(board.address) }
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .background(background, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            board.code,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

internal val BOARD_LOOKUP_ROW_HEIGHT = 48.dp
