package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun AttachmentRenameControls(
    file: ComposerAttachment,
    enabled: Boolean,
    onDone: () -> Unit = {},
    onUpdate: (ComposerAttachment) -> Unit,
) {
    val res = LocalResources.current

    val options = file.options
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    DisposableEffect(keyboard) {
        onDispose { keyboard?.hide() }
    }
    val originalBase = file.source.fileName.substringBeforeLast('.', file.source.fileName)
    val originalName = file.copy(options = options.copy(customName = null))
    val extension = attachmentUploadName(originalName).substringAfterLast('.', "")
    val name = options.customName ?: originalBase
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp).testTag("attachment-rename-controls"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                val customName = it.takeUnless { value -> value == originalBase }
                onUpdate(file.copy(options = options.copy(customName = customName)))
            },
            label = { Text(res.getString(L.ui_filename)) },
            suffix = { Text(if (extension.isEmpty()) "" else ".$extension") },
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            enabled = enabled && !options.removeFileName,
            isError = !validAttachmentName(name),
        )
        AttachmentRenameActions(file, enabled, onUpdate)
    }
}
