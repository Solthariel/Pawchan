package com.solthariel.pawchan.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

@Composable
internal fun BoardDirectoryCard(
    board: BoardListItem.Board,
    actions: BoardListActions,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier =
            modifier
                .fillMaxSize()
                .alpha(if (board.unavailable) UNAVAILABLE_BOARD_ALPHA else 1f)
                .clickable { actions.onBoardSelected(board.entry.address) }
                .semantics { contentDescription = "${board.codeLabel} ${board.entry.title}" }
                .testTag("board-card:${board.entry.address.board.value}"),
        shape = shape,
        color = boardCardSurface(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = BOARD_CARD_VERTICAL_PADDING),
            verticalArrangement = Arrangement.spacedBy(BOARD_CARD_CONTENT_GAP),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = BOARD_FAVORITE_TARGET),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = board.codeLabel,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                BoardStatus(board.entry, postsPerHour = null, unavailable = board.unavailable)
                UiToolbarButton(
                    kind = if (board.favorite) UiIconKind.BOOKMARK_FILLED else UiIconKind.BOOKMARK_ADD,
                    description =
                        if (board.favorite) {
                            "Убрать доску из избранного"
                        } else {
                            "Добавить доску в избранное"
                        },
                    active = board.favorite,
                    onClick = {
                        actions.onFavoriteChanged(board.entry.address, !board.favorite)
                    },
                )
            }
            Text(
                text = board.entry.title,
                modifier = Modifier.padding(start = 2.dp),
                maxLines = BOARD_CARD_TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private const val UNAVAILABLE_BOARD_ALPHA = 0.55f

internal fun boardCardSurface(
    background: Color,
    surface: Color,
): Color = if (background == Color.Black && surface == Color.Black) Color(AMOLED_BOARD_CARD) else surface

private const val AMOLED_BOARD_CARD = 0xff161616
