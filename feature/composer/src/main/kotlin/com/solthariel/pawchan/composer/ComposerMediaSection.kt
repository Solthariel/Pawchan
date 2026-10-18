package com.solthariel.pawchan.composer

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import coil3.ImageLoader

@Composable
internal fun ComposerMediaSection(
    draft: ComposerDraftState,
    imageLoader: ImageLoader,
    enabled: Boolean,
    error: String?,
    onChange: (ComposerDraftState) -> Unit,
) {
    ComposerAttachmentStrip(draft.attachments, imageLoader, enabled && !draft.filesLoading) { files ->
        onChange(draft.copy(attachments = files))
    }
    if (draft.filesLoading) CircularProgressIndicator()
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}
