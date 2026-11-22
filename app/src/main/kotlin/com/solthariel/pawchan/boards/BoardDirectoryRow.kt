package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun BoardCardGridRow(
    row: BoardListItem.GridRow,
    actions: BoardListActions,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(start = BOARD_ROW_INSET),
        horizontalArrangement = Arrangement.spacedBy(BOARD_CARD_GAP),
    ) {
        repeat(BOARD_GRID_COLUMNS) { index ->
            val board = row.boards.getOrNull(index)
            if (board == null) {
                Spacer(Modifier.weight(1f))
            } else {
                BoardDirectoryCard(board, actions, Modifier.weight(1f))
            }
        }
    }
}

private val BOARD_CARD_GAP = 8.dp
