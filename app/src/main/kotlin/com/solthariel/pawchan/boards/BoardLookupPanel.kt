package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.R.string as L

@Composable
internal fun BoardLookupPanel(
    content: BoardLookupContent,
    source: BoardLookupSource,
    select: (BoardAddress) -> Unit,
    retry: () -> Unit,
    tags: List<String> = emptyList(),
    selectTag: (String) -> Unit = {},
) {
    if (content.query.isNotBlank()) {
        BoardLookupSuggestions(
            content,
            source,
            select,
            retry,
            tags.filter { it.contains(content.query.trim().trim('/'), ignoreCase = true) },
            selectTag,
        )
    } else if (source.quickBoardsReady) {
        BoardLookupQuickBoards(content, select)
    }
}

@Composable
private fun BoardLookupQuickBoards(
    content: BoardLookupContent,
    select: (BoardAddress) -> Unit,
) {
    val res = LocalResources.current

    val columns = BOARD_LOOKUP_COLUMNS
    val favoriteRows = (content.favorites.size + columns - 1) / columns
    val recentRows = (content.recent.size + columns - 1) / columns
    val rows = boardLookupRowBudget(favoriteRows, recentRows)
    Column(Modifier.fillMaxWidth()) {
        BoardLookupGrid(
            content.favorites,
            columns,
            rows.favorites,
            select,
            "board-lookup-favorites",
            content.interactive,
        )
        if (rows.favorites > 0 && rows.recent > 0) HorizontalDivider()
        BoardLookupGrid(content.recent, columns, rows.recent, select, "board-lookup-recent", content.interactive)
        if (rows.favorites + rows.recent == 0) BoardLookupMessage(res.getString(L.ui_enter_board_code_or_name))
    }
}

internal const val BOARD_LOOKUP_COLUMNS = 5
