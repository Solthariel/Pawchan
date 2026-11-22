package com.solthariel.pawchan.boards

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Every lazy-list slot has a fixed height, including its category or grid gap. */
internal data class BoardListDimensions(
    val headerHeight: Dp,
    val rowHeight: Dp,
    val emptyHeight: Dp,
) {
    fun height(row: BoardListItem): Dp =
        when (row) {
            is BoardListItem.Header -> headerHeight + if (row.leadingGap) BOARD_SECTION_GAP else 0.dp
            is BoardListItem.GridRow -> rowHeight + BOARD_GRID_ROW_GAP
            is BoardListItem.Empty -> emptyHeight
        }
}

@Composable
internal fun boardListDimensions(): BoardListDimensions {
    val typography = MaterialTheme.typography
    return with(LocalDensity.current) {
        val labelHeight = maxOf(typography.titleMedium.lineHeight.toDp(), typography.titleLarge.lineHeight.toDp())
        val codeRowHeight = maxOf(BOARD_FAVORITE_TARGET, typography.bodyMedium.lineHeight.toDp())
        val cardContentHeight =
            codeRowHeight +
                typography.bodySmall.lineHeight.toDp() * BOARD_CARD_TITLE_MAX_LINES.toFloat() +
                BOARD_CARD_CONTENT_GAP +
                BOARD_CARD_VERTICAL_PADDING * 2
        BoardListDimensions(
            headerHeight = labelHeight + BOARD_HEADER_VERTICAL_PADDING,
            rowHeight = maxOf(BOARD_CARD_MIN_HEIGHT, cardContentHeight),
            emptyHeight = typography.bodyLarge.lineHeight.toDp() + BOARD_EMPTY_VERTICAL_PADDING,
        )
    }
}

internal val BOARD_SECTION_GAP = 8.dp
internal val BOARD_GRID_ROW_GAP = 8.dp
internal val BOARDS_BOTTOM_CONTENT_GAP = 18.dp
private val BOARD_HEADER_VERTICAL_PADDING = 12.dp
private val BOARD_EMPTY_VERTICAL_PADDING = 48.dp
internal val BOARD_FAVORITE_TARGET = 48.dp
internal val BOARD_CARD_MIN_HEIGHT = 96.dp
internal val BOARD_CARD_CONTENT_GAP = 4.dp
internal val BOARD_CARD_VERTICAL_PADDING = 4.dp
internal const val BOARD_CARD_TITLE_MAX_LINES = 2
