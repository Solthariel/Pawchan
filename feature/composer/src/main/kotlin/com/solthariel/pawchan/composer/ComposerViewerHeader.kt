package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerViewerHeader(
    file: ComposerAttachment,
    enabled: Boolean,
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
) {
    val close = stringResource(R.string.board_dialog_close)
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onRemove, enabled = enabled) { Text(stringResource(R.string.attachment_remove)) }
            IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = close }) {
                UiIcon(UiIconKind.CLOSE, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                file.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                file.typeAndSize,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                softWrap = false,
            )
            file.dimensions?.let { dimensions ->
                Text(
                    dimensions,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
