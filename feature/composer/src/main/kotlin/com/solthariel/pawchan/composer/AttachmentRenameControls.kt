package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun AttachmentRenameControls(
    file: ComposerAttachment,
    enabled: Boolean,
    onUpdate: (ComposerAttachment) -> Unit,
) {
    val options = file.options
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(file.id, enabled, options.removeFileName, windowFocused) {
        if (enabled && !options.removeFileName && windowFocused) {
            withFrameNanos {}
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    DisposableEffect(keyboard) {
        onDispose { keyboard?.hide() }
    }
    val originalBase = file.source.fileName.substringBeforeLast('.', file.source.fileName)
    val extension =
        options.reencoding?.let { if (it.jpeg) "jpg" else "png" }
            ?: file.source.fileName.substringAfterLast('.', "")
    val name = options.customName ?: originalBase
    val restore = stringResource(R.string.attachment_restore_name)
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                val customName = it.takeUnless { value -> value == originalBase }
                onUpdate(file.copy(options = options.copy(customName = customName)))
            },
            label = { Text(stringResource(R.string.attachment_rename)) },
            suffix = { Text(if (extension.isEmpty()) "" else ".$extension") },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            singleLine = true,
            enabled = enabled && !options.removeFileName,
            isError = !validAttachmentName(name),
        )
        IconButton(
            onClick = { onUpdate(file.copy(options = options.copy(customName = null))) },
            enabled = enabled && !options.removeFileName && options.customName != null,
            modifier = Modifier.semantics { contentDescription = restore },
        ) {
            UiIcon(UiIconKind.REFRESH, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp))
        }
    }
}
