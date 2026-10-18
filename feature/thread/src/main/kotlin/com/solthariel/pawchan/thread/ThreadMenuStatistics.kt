package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Plain text rows, deliberately not disabled/clickable menu actions. */
@Composable
internal fun ThreadMenuStatistics(content: ThreadUiState.Content) {
    val display = threadMenuStatistics(content)
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThreadMenuStatistic("Постов", display.posts)
        display.deleted?.let { ThreadMenuStatistic("Удалено", it) }
        ThreadMenuStatistic("Файлов", display.files)
        ThreadMenuStatistic("Размер файлов", display.size)
        ThreadMenuStatistic("Участников", display.participants)
    }
}

@Composable
private fun ThreadMenuStatistic(
    label: String,
    value: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
