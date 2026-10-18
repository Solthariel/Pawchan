package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialogStyle
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentRenameDialog(
    file: ComposerAttachment,
    enabled: Boolean,
    onUpdate: (ComposerAttachment) -> Unit,
    onDismiss: () -> Unit,
) {
    AttachmentFixedDialog(onDismiss) {
        Surface(modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(), shape = AppDialogStyle.shape) {
            Column(
                Modifier.padding(AppDialogStyle.contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.attachment_filename_title), style = MaterialTheme.typography.titleLarge)
                AttachmentRenameControls(file, enabled, onUpdate)
            }
        }
    }
}
