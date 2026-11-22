package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentRenameActions(
    file: ComposerAttachment,
    enabled: Boolean,
    onUpdate: (ComposerAttachment) -> Unit,
) {
    val options = file.options
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val restore = stringResource(R.string.attachment_restore_name)
    val remove = stringResource(R.string.attachment_remove_name)
    Row {
        IconButton(
            onClick = { onUpdate(file.copy(options = options.copy(customName = null, removeFileName = false))) },
            enabled = enabled && (options.customName != null || options.removeFileName),
            modifier = Modifier.semantics { contentDescription = restore },
        ) {
            UiIcon(UiIconKind.REFRESH, colors.onSurface, Modifier.size(24.dp))
        }
        IconToggleButton(
            checked = options.removeFileName,
            onCheckedChange = {
                focus.clearFocus()
                onUpdate(file.copy(options = options.copy(removeFileName = it)))
            },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = remove },
        ) {
            val tint =
                if (options.removeFileName) colors.primary else colors.onSurface
            UiIcon(UiIconKind.DELETE, tint, Modifier.size(24.dp))
        }
    }
}
