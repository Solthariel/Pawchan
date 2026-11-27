package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.solthariel.pawchan.R.string as L

@Composable
internal fun UsageStatisticsScreen(
    stats: UsageStatisticsRepository?,
    onBack: () -> Unit,
    bottomActions: AppBottomActions,
) {
    val res = LocalResources.current

    val repository =
        com.solthariel.pawchan.workspace.LocalWorkspace.current
            ?.appSettings
    val defaults = AppSettings()
    val app by (repository?.appSettings ?: flowOf(defaults)).collectAsStateWithLifecycle(defaults)
    val days by (stats?.days ?: flowOf(emptyList())).collectAsStateWithLifecycle(emptyList())
    val available by (stats?.available ?: flowOf(false)).collectAsStateWithLifecycle(stats != null)
    var period by rememberSaveable { mutableIntStateOf(0) }
    var writeFailed by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    val values = remember(days, period, today) { usagePeriodTotals(days, period, today) }
    AppNavigationWorkspace(bottomActions, AppBottomDestination.SETTINGS, floatingTop = true) {
        com.solthariel.pawchan.core.ui.FloatingTopChrome(toolbar = {
            SettingsPageToolbar(res.getString(L.ui_statistics), onBack) {
                UsageStatisticsMenu(
                    app.postCountEnabled,
                    { enabled ->
                        scope.launch {
                            val result =
                                repository?.updateAppSettings { current ->
                                    current.copy(behavior = current.behavior.copy(postCountEnabled = enabled))
                                }
                            writeFailed = result !is com.solthariel.pawchan.domain.model.DomainOutcome.Success<*>
                        }
                    },
                ) { confirm = true }
            }
            UsagePeriodOptions(period) { period = it }
            if (available) UsageDataStart(days)
        }) {
            if (available) {
                UsageStatisticsList(values, Modifier.fillMaxSize())
            } else {
                Text(
                    res.getString(L.ui_failed_to_read_or_save),
                    Modifier
                        .padding(
                            top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                        ).padding(20.dp),
                )
            }
        }
    }
    if (writeFailed) CacheNotice(res.getString(L.ui_failed_to_save_postcount_setting)) { writeFailed = false }
    if (confirm) {
        UsageResetDialog({ confirm = false }) {
            confirm = false
            scope.launch { stats?.reset() }
        }
    }
}

@Composable
private fun UsageStatisticsList(
    values: Map<UsageMetric, Long>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier,
        contentPadding =
            PaddingValues(
                top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current,
                bottom = LocalBottomOverlayInset.current + 8.dp,
            ),
    ) {
        items(UsageMetric.entries, key = { it.name }) { metric ->
            UsageValueRow(metric.usageLabel(), usageValue(metric, values[metric] ?: 0L))
        }
    }
}

@Composable
internal fun UsageValueRow(
    label: String,
    value: String,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f).padding(end = 16.dp), style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun UsageDataStart(days: List<com.solthariel.pawchan.domain.statistics.UsageDay>) {
    val res = LocalResources.current

    val first = days.minOfOrNull { it.epochDay }
    val label =
        if (first == null) {
            res.getString(L.ui_no_saved_entries_yet)
        } else {
            val date = LocalDate.ofEpochDay(first)
            "Первые сохранённые данные: ${date.format(
                java.time.format.DateTimeFormatter
                    .ofPattern("dd.MM.yyyy"),
            )}"
        }
    Text(
        label,
        Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
