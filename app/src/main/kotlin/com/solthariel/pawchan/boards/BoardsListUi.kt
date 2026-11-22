package com.solthariel.pawchan.boards

import com.solthariel.pawchan.core.ui.FastScrollItemSizes
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.FAVORITES_BOARD_CATEGORY_KEY
import com.solthariel.pawchan.domain.model.BoardAddress
import kotlin.math.roundToInt

internal data class BoardsListUi(
    val rows: List<BoardListItem>,
    val dimensions: BoardListDimensions,
    val itemSizes: FastScrollItemSizes,
)

internal sealed interface BoardListItem {
    val key: String
    val contentType: String

    data class Header(
        val categoryKey: String,
        val label: String,
        val expanded: Boolean,
        val enabled: Boolean,
        val leadingGap: Boolean,
    ) : BoardListItem {
        override val key: String = "category:$categoryKey"
        override val contentType: String = "board-category-header"
    }

    data class GridRow(
        override val key: String,
        val boards: List<Board>,
    ) : BoardListItem {
        override val contentType: String = "board-grid-row"
    }

    data class Empty(
        val favoritesOnly: Boolean,
    ) : BoardListItem {
        override val key: String = "boards-empty"
        override val contentType: String = "boards-empty"
    }

    data class Board(
        val entry: BoardDirectoryEntry,
        val favorite: Boolean,
        val unavailable: Boolean,
    ) {
        val codeLabel: String = "/${entry.address.board.value}/"
    }
}

/** Called on Dispatchers.Default, never from the scrolling item content. */
internal fun BoardsUiState.Content.prepareBoardsList(
    query: String,
    dimensions: BoardListDimensions,
    pixelDensity: Float,
    favoritesOnly: Boolean = false,
    categoryFilter: String? = null,
): BoardsListUi {
    val searching = query.isNotBlank()
    val visible =
        filterBoards(boards, query).filter { board ->
            !favoritesOnly || board.address in favoriteBoards
        }
    val source = this
    val rows =
        buildList<BoardListItem> {
            val categories =
                boardCategories(visible).filter { categoryFilter.isNullOrEmpty() || it.title == categoryFilter }
            for (category in categories) {
                source.appendCategory(this, category, searching, favoritesOnly, categoryFilter != null)
            }
            if (categories.isEmpty()) add(BoardListItem.Empty(favoritesOnly = favoritesOnly && !searching))
        }
    val heights =
        IntArray(rows.size) { index ->
            (dimensions.height(rows[index]).value * pixelDensity).roundToInt().coerceAtLeast(1)
        }
    return BoardsListUi(rows, dimensions, FastScrollItemSizes(heights))
}

private fun BoardsUiState.Content.appendCategory(
    rows: MutableList<BoardListItem>,
    category: BoardCategoryUi,
    searching: Boolean,
    favoritesOnly: Boolean,
    chipMode: Boolean,
) {
    val expanded =
        (chipMode || searching || favoritesOnly || category.title in expandedCategories) && category.boards.isNotEmpty()
    if (!chipMode) {
        rows.add(
            BoardListItem.Header(
                categoryKey = category.title,
                label = "${category.title}  ${category.boards.size}",
                expanded = expanded,
                enabled = !searching && !favoritesOnly && category.boards.isNotEmpty(),
                leadingGap = rows.isNotEmpty(),
            ),
        )
    }
    if (!expanded) return

    category.boards.chunked(BOARD_GRID_COLUMNS).forEachIndexed { rowIndex, entries ->
        val cards =
            entries.map { entry ->
                BoardListItem.Board(
                    entry = entry,
                    favorite = entry.address in favoriteBoards,
                    unavailable = entry.address in unavailableBoards,
                )
            }
        val first = cards.first()
        rows.add(
            BoardListItem.GridRow(
                key = "board-grid:${category.title}:$rowIndex:${first.entry.address.board.value}",
                boards = cards,
            ),
        )
    }
}

internal data class BoardListActions(
    val onBoardSelected: (BoardAddress) -> Unit,
    val onFavoriteChanged: (BoardAddress, Boolean) -> Unit,
)

internal const val BOARD_GRID_COLUMNS = 3
internal const val FAVORITES_CATEGORY_KEY = FAVORITES_BOARD_CATEGORY_KEY
