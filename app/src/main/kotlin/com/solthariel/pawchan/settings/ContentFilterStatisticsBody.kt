package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.domain.filter.ContentFilterBoardStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ContentFilterStatisticsBody(
    statistics: ContentFilterStatistics,
    resetFailed: Boolean,
) {
    val res = LocalResources.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (statistics.available) {
            FilterStatisticsSummary(statistics)
            statistics.boards.forEach { board -> FilterStatisticsBoard(board) }
        } else {
            Text(
                if (statistics.storageFailure) {
                    res.getString(
                        L.ui_statistics_unavailable,
                    )
                } else {
                    res.getString(L.ui_statistics_are_loading_2346)
                },
            )
        }
        if (statistics.storageFailure || resetFailed) {
            TransientNotice(res.getString(L.ui_statistics_storage_error_data_may))
        }
    }
}

@Composable
private fun FilterStatisticsSummary(statistics: ContentFilterStatistics) {
    val res = LocalResources.current

    FilterStatisticsGroup(
        if (statistics.boards.isEmpty()) null else res.getString(L.ui_overall_statistics),
        listOf(
            res.getString(L.ui_total_triggers) to statistics.total.toString(),
            res.getString(L.ui_op_posts) to statistics.originalPosts.toString(),
            res.getString(L.ui_replies) to statistics.replies.toString(),
            res.getString(L.ui_boards_1533) to statistics.boards.size.toString(),
            res.getString(L.ui_rules_with_triggers) to statistics.rules.size.toString(),
        ),
    )
}

@Composable
private fun FilterStatisticsBoard(board: ContentFilterBoardStatistics) {
    val res = LocalResources.current

    FilterStatisticsGroup(
        if (board.site == "dvach") "Двач /${board.board}/" else "${board.site} /${board.board}/",
        listOf(
            res.getString(L.ui_triggers_bede) to board.total.toString(),
            res.getString(L.ui_op_posts) to board.originalPosts.toString(),
            res.getString(L.ui_replies) to board.replies.toString(),
        ),
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(res.getString(L.ui_last_trigger), style = MaterialTheme.typography.bodySmall)
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
                Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(row.first, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(row.second, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
