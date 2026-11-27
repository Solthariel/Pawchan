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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.AppDialog
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.DialogActions
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.feature.thread.R.string as L

@Composable
internal fun ThreadReportDialog(
    controller: ThreadReportController,
    ids: Set<Long>,
    onClose: () -> Unit,
) {
    val res = LocalResources.current

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
                res.getString(L.ui_report_on_posts, ids.size),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedTextField(
                reason,
                { reason = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(res.getString(L.ui_reason)) },
                enabled = !report.sending,
                maxLines = 4,
            )
            ThreadReportStatus(report)
            DialogActions(
                dismiss = { TextButton(onClick = close) { Text(res.getString(L.ui_cancel)) } },
                confirm = {
                    TextButton(
                        enabled = !report.sending && report.sent == 0 && reason.isNotBlank() && ids.isNotEmpty(),
                        onClick = { controller.submit(ids.sorted(), reason) },
                    ) { Text(res.getString(L.ui_send)) }
                },
            )
        }
    }
}

@Composable
private fun ThreadReportStatus(report: ThreadReportState) {
    val res = LocalResources.current

    val message =
        when {
            report.sending -> res.getString(L.ui_being_sent)
            report.sent > 0 -> res.getString(L.ui_sent, report.sent)
            else -> report.failure
        }
    TransientNotice(message)
}
