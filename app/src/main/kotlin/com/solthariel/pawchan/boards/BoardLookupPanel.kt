package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.solthariel.pawchan.domain.model.BoardAddress

@Composable
internal fun BoardLookupPanel(
    content: BoardLookupContent,
    source: BoardLookupSource,
    select: (BoardAddress) -> Unit,
    retry: () -> Unit,
) {
    if (content.query.isNotBlank()) {
        BoardLookupSuggestions(content.suggestions, source, select, retry)
    } else {
        BoardLookupQuickBoards(content, select)
    }
}

@Composable
private fun BoardLookupQuickBoards(
    content: BoardLookupContent,
    select: (BoardAddress) -> Unit,
) {
    val columns = BOARD_LOOKUP_COLUMNS
    val favoriteRows = (content.favorites.size + columns - 1) / columns
    val recentRows = (content.recent.size + columns - 1) / columns
    val rows = boardLookupRowBudget(favoriteRows, recentRows)
    Column(Modifier.fillMaxWidth()) {
        BoardLookupGrid(content.favorites, columns, rows.favorites, select, "board-lookup-favorites")
        if (rows.favorites > 0 && rows.recent > 0) HorizontalDivider()
        BoardLookupGrid(content.recent, columns, rows.recent, select, "board-lookup-recent")
        if (rows.favorites + rows.recent == 0) BoardLookupMessage("Введите код или название доски")
    }
}

internal const val BOARD_LOOKUP_COLUMNS = 5
