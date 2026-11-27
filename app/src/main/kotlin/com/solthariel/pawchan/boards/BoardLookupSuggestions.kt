package com.solthariel.pawchan.boards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.R.string as L

@Composable
internal fun BoardLookupSuggestions(
    content: BoardLookupContent,
    source: BoardLookupSource,
    select: (BoardAddress) -> Unit,
    retry: () -> Unit,
    tags: List<String> = emptyList(),
    selectTag: (String) -> Unit = {},
) {
    val boards = content.suggestions
    if (boards.isEmpty() && tags.isEmpty()) {
        BoardLookupEmpty(source, retry, content.interactive)
    } else {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .heightIn(max = BOARD_LOOKUP_ROW_HEIGHT * BOARD_LOOKUP_MAX_ROWS)
                .testTag("board-lookup-results"),
        ) {
            items(tags, key = { "tag:$it" }) { tag ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(BOARD_LOOKUP_ROW_HEIGHT)
                        .clickable(enabled = content.interactive) { selectTag(tag) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        tag,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            items(boards, key = { it.key }) { board ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(BOARD_LOOKUP_ROW_HEIGHT)
                        .clickable(enabled = content.interactive) { select(board.address) }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        board.description,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardLookupEmpty(
    source: BoardLookupSource,
    retry: () -> Unit,
    enabled: Boolean,
) {
    val res = LocalResources.current

    when {
        source.loading -> {
            BoardLookupMessage(res.getString(L.ui_loading_boards))
        }

        source.failed -> {
            Row(
                Modifier.fillMaxWidth().height(BOARD_LOOKUP_ROW_HEIGHT).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    res.getString(L.ui_failed_to_load_boards),
                    Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = retry, enabled = enabled) { Text(res.getString(L.ui_retry)) }
            }
        }

        else -> {
            BoardLookupMessage(res.getString(L.ui_nothing_found))
        }
    }
}

@Composable
internal fun BoardLookupMessage(message: String) {
    Box(
        Modifier.fillMaxWidth().height(BOARD_LOOKUP_ROW_HEIGHT).padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
