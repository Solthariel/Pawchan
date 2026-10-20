package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.DialogActions

@Composable
internal fun ThreadReportDialog(
    controller: ThreadReportController,
    ids: Set<Long>,
    onClose: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    val report by controller.state.collectAsStateWithLifecycle()
    val close = {
        controller.cancel()
        onClose()
    }
    AppDialog(close) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(AppDialogStyle.contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Жалоба на посты: ${ids.size}",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedTextField(
                reason,
                { reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Причина") },
                enabled = !report.sending,
                maxLines = 4,
            )
            ThreadReportStatus(report)
            DialogActions(
                dismiss = { TextButton(onClick = close) { Text("Отмена") } },
                confirm = {
                    TextButton(
                        enabled = !report.sending && report.sent == 0 && reason.isNotBlank() && ids.isNotEmpty(),
                        onClick = { controller.submit(ids.sorted(), reason) },
                    ) { Text("Отправить") }
                },
            )
        }
    }
}

@Composable
private fun ThreadReportStatus(report: ThreadReportState) {
    if (report.sending) Text("Отправляется…", style = MaterialTheme.typography.bodyMedium)
    if (report.sent > 0) Text("Отправлено: ${report.sent}", style = MaterialTheme.typography.bodyMedium)
    report.failure?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
