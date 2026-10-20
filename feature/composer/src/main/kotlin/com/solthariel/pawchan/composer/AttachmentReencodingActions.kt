package com.solthariel.pawchan.composer

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.solthariel.pawchan.core.ui.DialogActions
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentReencodingActions(
    enabled: Boolean,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val resetLabel = stringResource(R.string.attachment_reencode_reset)
    DialogActions(
        neutral = {
            TextButton(
                enabled = enabled,
                onClick = onReset,
                modifier = Modifier.semantics { contentDescription = resetLabel },
            ) { Text("Сбросить") }
        },
        dismiss = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
        confirm = {
            TextButton(enabled = enabled, onClick = onConfirm) {
                Text(stringResource(R.string.attachment_reencode_apply))
            }
        },
    )
}
