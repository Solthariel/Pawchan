package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.core.ui.DialogActions
import com.solthariel.pawchan.core.ui.modalLayerSurface
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentRenameDialog(
    file: ComposerAttachment,
    enabled: Boolean,
    onUpdate: (ComposerAttachment) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(file.id) { mutableStateOf(file) }
    val save = {
        if (enabled && draft.validName) {
            onUpdate(draft)
            onDismiss()
        }
    }
    AttachmentFixedDialog(onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().modalLayerSurface(AppDialogStyle.shape),
            shape = AppDialogStyle.shape,
        ) {
            Column(
                Modifier.padding(AppDialogStyle.contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.attachment_filename_title), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(AppDialogStyle.titleBottomSpacing))
                AttachmentRenameControls(draft, enabled, onDone = save) { draft = it }
                DialogActions(
                    dismiss = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
                    confirm = {
                        TextButton(enabled = enabled && draft.validName, onClick = save) {
                            Text(stringResource(R.string.attachment_name_save))
                        }
                    },
                )
            }
        }
    }
}
