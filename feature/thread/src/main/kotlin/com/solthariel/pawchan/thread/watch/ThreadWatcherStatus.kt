package com.solthariel.pawchan.thread.watch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.thread.R

@Composable
internal fun ThreadWatcherStatus(
    state: ThreadWatcherUiState,
    onToggle: () -> Unit,
) {
    val text = watcherStatusText(state)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color =
                if (state.error == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
        )
        TextButton(onClick = onToggle) {
            Text(
                stringResource(
                    if (state.enabled) R.string.watcher_stop else R.string.watcher_start,
                ),
            )
        }
    }
}

@Composable
private fun watcherStatusText(state: ThreadWatcherUiState): String =
    when {
        state.stopReason == ThreadWatcherStopReason.CLOSED -> {
            stringResource(R.string.watcher_stopped_closed)
        }

        state.stopReason == ThreadWatcherStopReason.DELETED -> {
            stringResource(R.string.watcher_stopped_deleted)
        }

        !state.enabled -> {
            stringResource(R.string.watcher_disabled)
        }

        !state.active -> {
            stringResource(R.string.watcher_paused)
        }

        state.checking -> {
            stringResource(R.string.watcher_checking)
        }

        state.error != null -> {
            stringResource(R.string.watcher_error)
        }

        state.newPosts > 0 -> {
            stringResource(R.string.watcher_new_posts, state.newPosts)
        }

        else -> {
            stringResource(R.string.watcher_waiting)
        }
    }
