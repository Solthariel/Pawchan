package com.solthariel.pawchan.boards

import com.solthariel.pawchan.core.ui.FastScrollItemSizes
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.FAVORITES_BOARD_CATEGORY_KEY
import com.solthariel.pawchan.domain.model.BoardId
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

    data class Board(
        override val key: String,
        val entry: BoardDirectoryEntry,
        val favorite: Boolean,
        val postsPerHour: Int?,
        val unavailable: Boolean,
        val position: BoardGroupPosition,
    ) : BoardListItem {
        val codeLabel: String = "/${entry.address.board.value}/"
        override val contentType: String = "board-directory-row"
    }

    data object Empty : BoardListItem {
        override val key: String = "boards-empty"
        override val contentType: String = "boards-empty"
    }
}

internal enum class BoardGroupPosition {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST,
    ;

    val first: Boolean
        get() = this == FIRST || this == SINGLE
    val last: Boolean
        get() = this == LAST || this == SINGLE
}

/** Called on Dispatchers.Default, never from the scrolling item content. */
internal fun BoardsUiState.Content.prepareBoardsList(
    query: String,
    favoriteLabel: String,
    dimensions: BoardListDimensions,
    pixelDensity: Float,
): BoardsListUi {
    val visible = filterBoards(boards, query)
    val searching = query.isNotBlank()
    val rows =
        buildList<BoardListItem> {
            if (!searching) {
                appendCategory(
                    BoardCategoryUi(favoriteLabel, boards.filter { it.address.board in favoriteBoards }),
                    this@prepareBoardsList,
                    searching = false,
                    categoryKey = FAVORITES_CATEGORY_KEY,
                )
            }
            for (category in boardCategories(visible)) {
                appendCategory(category, this@prepareBoardsList, searching, category.title)
            }
            if (visible.isEmpty()) add(BoardListItem.Empty)
        }
    val heights =
        IntArray(rows.size) { index ->
            (dimensions.height(rows[index]).value * pixelDensity).roundToInt().coerceAtLeast(1)
        }
    return BoardsListUi(rows, dimensions, FastScrollItemSizes(heights))
}

private fun MutableList<BoardListItem>.appendCategory(
    category: BoardCategoryUi,
    state: BoardsUiState.Content,
    searching: Boolean,
    categoryKey: String,
) {
    val expanded = (searching || categoryKey in state.expandedCategories) && category.boards.isNotEmpty()
    add(
        BoardListItem.Header(
            categoryKey = categoryKey,
            label = "${category.title}  ${category.boards.size}",
            expanded = expanded,
            enabled = !searching && category.boards.isNotEmpty(),
            leadingGap = isNotEmpty(),
        ),
    )
    if (!expanded) return
    for ((index, entry) in category.boards.withIndex()) {
        add(state.boardListItem(categoryKey, entry, boardGroupPosition(index, category.boards.lastIndex)))
    }
}

private fun BoardsUiState.Content.boardListItem(
    categoryKey: String,
    entry: BoardDirectoryEntry,
    position: BoardGroupPosition,
): BoardListItem.Board =
    BoardListItem.Board(
        key = "board:$categoryKey:${entry.address.site.value}/${entry.address.board.value}",
        entry = entry,
        favorite = entry.address.board in favoriteBoards,
        postsPerHour = boardSpeeds[entry.address.board],
        unavailable = entry.address.board in unavailableBoards,
        position = position,
    )

private fun boardGroupPosition(
    index: Int,
    lastIndex: Int,
): BoardGroupPosition =
    when {
        lastIndex == 0 -> BoardGroupPosition.SINGLE
        index == 0 -> BoardGroupPosition.FIRST
        index == lastIndex -> BoardGroupPosition.LAST
        else -> BoardGroupPosition.MIDDLE
    }

internal data class BoardListActions(
    val onBoardSelected: (BoardId) -> Unit,
    val onFavoriteChanged: (BoardId, Boolean) -> Unit,
)

internal const val FAVORITES_CATEGORY_KEY = FAVORITES_BOARD_CATEGORY_KEY
