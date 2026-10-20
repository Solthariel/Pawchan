package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterBoardStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun ContentFilterStatisticsBody(
    statistics: ContentFilterStatistics,
    resetFailed: Boolean,
) {
    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (statistics.available) {
            FilterStatisticsSummary(statistics)
            statistics.boards.forEach { board -> FilterStatisticsBoard(board) }
        } else {
            Text(if (statistics.storageFailure) "Статистика недоступна" else "Статистика загружается…")
        }
        if (statistics.storageFailure || resetFailed) {
            Text("Ошибка хранения статистики: данные могут быть неполными", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun FilterStatisticsSummary(statistics: ContentFilterStatistics) {
    FilterStatisticsGroup(
        if (statistics.boards.isEmpty()) null else "Общая статистика",
        listOf(
            "Всего срабатываний" to statistics.total.toString(),
            "ОП-посты" to statistics.originalPosts.toString(),
            "Ответы" to statistics.replies.toString(),
            "Досок" to statistics.boards.size.toString(),
            "Правил со срабатываниями" to statistics.rules.size.toString(),
        ),
    )
}

@Composable
private fun FilterStatisticsBoard(board: ContentFilterBoardStatistics) {
    FilterStatisticsGroup(
        if (board.site == "dvach") "Двач /${board.board}/" else "${board.site} /${board.board}/",
        listOf(
            "Срабатываний" to board.total.toString(),
            "ОП-посты" to board.originalPosts.toString(),
            "Ответы" to board.replies.toString(),
        ),
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Последнее срабатывание", style = MaterialTheme.typography.bodySmall)
        Text(formatFilterStatisticsTime(board.lastHitMillis), style = MaterialTheme.typography.bodyMedium)
    }
}

internal fun formatFilterStatisticsTime(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): String = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss").format(Instant.ofEpochMilli(millis).atZone(zone))

@Composable
private fun FilterStatisticsGroup(
    title: String?,
    rows: List<Pair<String, String>>,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleSmall)
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.first, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(row.second, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
