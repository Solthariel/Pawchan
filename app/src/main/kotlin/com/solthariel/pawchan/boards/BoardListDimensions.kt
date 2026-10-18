package com.solthariel.pawchan.boards

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Every slot has one declared height, including group gaps and separators. */
internal data class BoardListDimensions(
    val headerHeight: Dp,
    val rowHeight: Dp,
    val emptyHeight: Dp,
) {
    fun height(row: BoardListItem): Dp =
        when (row) {
            is BoardListItem.Header -> headerHeight + if (row.leadingGap) BOARD_SECTION_GAP else 0.dp
            is BoardListItem.Board -> rowHeight + if (row.position.first) BOARD_GROUP_GAP else BOARD_SEPARATOR_HEIGHT
            BoardListItem.Empty -> emptyHeight
        }
}

@Composable
internal fun boardListDimensions(): BoardListDimensions {
    val typography = MaterialTheme.typography
    return with(LocalDensity.current) {
        val labelHeight = maxOf(typography.titleMedium.lineHeight.toDp(), typography.titleLarge.lineHeight.toDp())
        val textHeight = maxOf(labelHeight, typography.bodyLarge.lineHeight.toDp())
        val buttonHeight =
            maxOf(
                BOARD_BUTTON_MIN_HEIGHT,
                typography.labelLarge.lineHeight.toDp() + BOARD_BUTTON_PADDING,
            )
        BoardListDimensions(
            headerHeight = labelHeight + BOARD_HEADER_VERTICAL_PADDING,
            rowHeight = maxOf(textHeight, buttonHeight) + BOARD_ROW_VERTICAL_PADDING,
            emptyHeight = typography.bodyLarge.lineHeight.toDp() + BOARD_EMPTY_VERTICAL_PADDING,
        )
    }
}

internal val BOARD_SECTION_GAP = 18.dp
internal val BOARD_GROUP_GAP = 8.dp
internal val BOARD_SEPARATOR_HEIGHT = 1.dp
internal val BOARDS_BOTTOM_CONTENT_GAP = 18.dp
private val BOARD_HEADER_VERTICAL_PADDING = 12.dp
private val BOARD_ROW_VERTICAL_PADDING = 20.dp
private val BOARD_BUTTON_MIN_HEIGHT = 48.dp
private val BOARD_BUTTON_PADDING = 16.dp
private val BOARD_EMPTY_VERTICAL_PADDING = 48.dp
