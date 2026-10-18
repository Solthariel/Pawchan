package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentEditorFooter(
    file: ComposerAttachment,
    enabled: Boolean,
    onEdit: (ComposerAttachment) -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        AttachmentOptionsPanel(file, enabled, onUpdate = onEdit)
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        TextButton(
            onClick = onRemove,
            enabled = enabled,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(
                painterResource(R.drawable.ic_attachment_delete),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.attachment_remove))
        }
    }
}
