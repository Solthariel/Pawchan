package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry

@Composable
internal fun BoardStatus(
    board: BoardDirectoryEntry,
    postsPerHour: Int?,
    unavailable: Boolean,
) {
    val status = boardStatus(board, postsPerHour, unavailable)
    status.postsPerHour?.let { speed ->
        Text(
            text = stringResource(R.string.board_speed_short, speed),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
    status.lock?.let { lock ->
        val description =
            stringResource(
                when (lock) {
                    BoardLock.RESTRICTED -> R.string.board_unavailable_short
                    BoardLock.READ_ONLY -> R.string.board_read_only
                },
            )
        UiIcon(
            kind = UiIconKind.LOCK,
            color =
                when (lock) {
                    BoardLock.RESTRICTED -> MaterialTheme.colorScheme.onSurfaceVariant
                    BoardLock.READ_ONLY -> MaterialTheme.colorScheme.error
                },
            modifier = Modifier.size(15.dp).semantics { contentDescription = description },
        )
    }
}

internal data class BoardStatusUi(
    val postsPerHour: Int?,
    val lock: BoardLock?,
)

internal enum class BoardLock {
    RESTRICTED,
    READ_ONLY,
}

internal fun boardStatus(
    board: BoardDirectoryEntry,
    postsPerHour: Int?,
    unavailable: Boolean,
): BoardStatusUi =
    when {
        unavailable -> BoardStatusUi(postsPerHour = null, lock = BoardLock.RESTRICTED)
        !board.postingEnabled -> BoardStatusUi(postsPerHour = null, lock = BoardLock.READ_ONLY)
        else -> BoardStatusUi(postsPerHour = postsPerHour, lock = null)
    }
