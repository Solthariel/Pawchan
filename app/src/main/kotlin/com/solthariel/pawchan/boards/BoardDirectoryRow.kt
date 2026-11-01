package com.solthariel.pawchan.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

@Composable
internal fun BoardDirectoryRow(
    row: BoardListItem.Board,
    actions: BoardListActions,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape =
            RoundedCornerShape(
                topStart = if (row.position.first) BOARD_GROUP_RADIUS else 0.dp,
                topEnd = if (row.position.first) BOARD_GROUP_RADIUS else 0.dp,
                bottomStart = if (row.position.last) BOARD_GROUP_RADIUS else 0.dp,
                bottomEnd = if (row.position.last) BOARD_GROUP_RADIUS else 0.dp,
            ),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (!row.position.first) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = BOARD_ROW_INSET),
                    thickness = BOARD_SEPARATOR_HEIGHT,
                )
            }
            BoardRowContent(row, actions)
        }
    }
}

@Composable
private fun BoardRowContent(
    row: BoardListItem.Board,
    actions: BoardListActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .alpha(if (row.unavailable) UNAVAILABLE_BOARD_ALPHA else 1f)
                .clip(RoundedCornerShape(20.dp))
                .clickable { actions.onBoardSelected(row.entry.address.board) }
                .padding(start = BOARD_ROW_INSET, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.codeLabel,
            modifier = Modifier.width(BOARD_CODE_WIDTH),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = row.entry.title,
            modifier = Modifier.weight(1f).padding(start = BOARD_TITLE_GAP),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
        )
        BoardStatus(row.entry, row.postsPerHour, row.unavailable)
        IconButton(
            onClick = { actions.onFavoriteChanged(row.entry.address.board, !row.favorite) },
            modifier =
                Modifier.semantics {
                    contentDescription =
                        if (row.favorite) "Убрать доску из избранного" else "Добавить доску в избранное"
                    selected = row.favorite
                },
        ) {
            UiIcon(
                if (row.favorite) UiIconKind.STAR_FILLED else UiIconKind.STAR,
                if (row.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                Modifier.size(24.dp),
            )
        }
        UiForwardIndicator(Modifier.padding(end = 14.dp))
    }
}

private val BOARD_GROUP_RADIUS = 24.dp
private const val UNAVAILABLE_BOARD_ALPHA = 0.55f
