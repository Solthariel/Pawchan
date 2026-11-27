package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.R.string as L

/** One settings action; results live only in the shared, dismissible menu-style window. */
@Composable
internal fun MirrorAvailabilityResults(
    presentation: MirrorAvailabilityPresentation,
    checking: Boolean,
    onCheck: () -> Unit,
) {
    val res = LocalResources.current

    var open by rememberSaveable { mutableStateOf(false) }
    SettingsRow(res.getString(L.ui_check_mirrors), null, onClick = {
        open = true
        if (!checking) onCheck()
    }) {
        UiIcon(UiIconKind.REFRESH, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(24.dp))
    }
    if (open) {
        AppAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(res.getString(L.ui_checking_mirrors)) },
            text = { MirrorCheckResults(presentation, checking) },
            confirmButton = {
                TextButton(onClick = { open = false }) { Text(res.getString(L.ui_close)) }
            },
            dismissButton = {
                TextButton(onClick = onCheck, enabled = !checking) { Text(res.getString(L.ui_retry)) }
            },
        )
    }
}

@Composable
private fun MirrorCheckResults(
    presentation: MirrorAvailabilityPresentation,
    checking: Boolean,
) {
    val res = LocalResources.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (checking) {
            Text(res.getString(L.ui_checking_availability))
        } else if (presentation.rows.isEmpty()) {
            Text(res.getString(L.ui_no_check_results))
        } else {
            presentation.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                MirrorCheckRow(row)
            }
        }
        Text(
            res.getString(L.ui_average_time_of_successful_http),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MirrorCheckRow(row: MirrorStatusRow) {
    FlowRow(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(row.host, Modifier.padding(end = 12.dp))
        Text(
            listOfNotNull(row.caption, row.responseCaption?.removePrefix("HTTP-проверка: ")).joinToString(" — "),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
