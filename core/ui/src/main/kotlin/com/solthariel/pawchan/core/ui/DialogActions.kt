package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Neutral action stays left; cancel precedes confirmation at the right, wrapping if needed. */
@Composable
fun DialogActions(
    confirm: (@Composable () -> Unit)?,
    dismiss: (@Composable () -> Unit)? = null,
    neutral: (@Composable () -> Unit)? = null,
) {
    Row(Modifier.fillMaxWidth().testTag("dialog-actions"), verticalAlignment = Alignment.CenterVertically) {
        neutral?.invoke()
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        ) {
            dismiss?.invoke()
            confirm?.invoke()
        }
    }
}
