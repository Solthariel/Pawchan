package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

/** Shared alert layout; no empty button strip in immediate-selection dialogs. */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    dismissButton: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    AppDialog(onDismissRequest, properties = properties) {
        Column(Modifier.padding(AppDialogStyle.contentPadding), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            title?.let { ProvideTextStyle(MaterialTheme.typography.titleLarge, it) }
            text?.let {
                Box(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium, it)
                }
            }
            if (dismissButton != null || confirmButton != null) {
                DialogActions(confirmButton, dismissButton)
            }
        }
    }
}
