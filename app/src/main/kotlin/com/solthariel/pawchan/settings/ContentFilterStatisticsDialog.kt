package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.launch
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun ContentFilterStatisticsDialog(
    statistics: ContentFilterStatistics,
    repository: ContentFilterStatisticsRepository?,
    close: () -> Unit,
) {
    val res = LocalResources.current

    var confirm by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    if (!confirm) {
        AlertDialog(
            onDismissRequest = close,
            title = { Text(res.getString(L.ui_autohide_statistics)) },
            text = { ContentFilterStatisticsBody(statistics, failed) },
            confirmButton = { TextButton(onClick = close) { Text(res.getString(L.ui_close)) } },
            dismissButton = {
                TextButton(
                    enabled = repository != null && statistics.available && !resetting,
                    onClick = { confirm = true },
                ) {
                    Text(res.getString(L.ui_reset))
                }
            },
        )
    }
    if (confirm) {
        FilterStatisticsResetConfirmation({ confirm = false }) {
            confirm = false
            scope.launch {
                resetting = true
                try {
                    failed = repository?.reset() !is DomainOutcome.Success
                } finally {
                    resetting = false
                }
            }
        }
    }
}

@Composable
private fun FilterStatisticsResetConfirmation(
    close: () -> Unit,
    reset: () -> Unit,
) {
    val res = LocalResources.current

    com.solthariel.pawchan.core.ui.AppAlertDialog(
        onDismissRequest = close,
        title = { Text(res.getString(L.ui_reset_statistics_8e37)) },
        text = { Text(res.getString(L.ui_counters_will_be_reset_rules)) },
        confirmButton = { TextButton(onClick = reset) { Text(res.getString(L.ui_reset_counters)) } },
        dismissButton = { TextButton(onClick = close) { Text(res.getString(L.ui_cancel)) } },
    )
}
