package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.domain.statistics.UsageMetric
import com.solthariel.pawchan.domain.statistics.UsageStatisticsRepository
import com.solthariel.pawchan.domain.statistics.hasLegacyUsage
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
internal fun UsageStatisticsScreen(
    stats: UsageStatisticsRepository?,
    onBack: () -> Unit,
    bottomActions: AppBottomActions,
) {
    val repository =
        com.solthariel.pawchan.workspace.LocalWorkspace.current
            ?.appSettings
    val app by (
        repository?.appSettings ?: flowOf(
            com.solthariel.pawchan.domain.settings
                .AppSettings(),
        )
    ).collectAsStateWithLifecycle(
        com.solthariel.pawchan.domain.settings
            .AppSettings(),
    )
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
            SettingsPageToolbar("Статистика", onBack) {
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
                UsageStatisticsList(values, days.any { it.hasLegacyUsage() }, Modifier.fillMaxSize())
            } else {
                Text(
                    "Не удалось прочитать или сохранить статистику",
                    Modifier.padding(top = com.solthariel.pawchan.core.ui.LocalTopOverlayInset.current).padding(20.dp),
                )
            }
        }
    }
    if (writeFailed) CacheNotice("Не удалось сохранить настройку /postcount") { writeFailed = false }
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
    hasLegacy: Boolean,
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
        if (hasLegacy) item(key = "legacy-usage-notice") { LegacyUsageNotice() }
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
private fun LegacyUsageNotice() {
    Text(
        "Старые итоги сохранены. Уникальность данных до обновления восстановить нельзя.",
        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun UsageDataStart(days: List<com.solthariel.pawchan.domain.statistics.UsageDay>) {
    val first = days.minOfOrNull { it.epochDay }
    val label =
        if (first == null) {
            "Сохранённых записей пока нет"
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
