package com.solthariel.pawchan.composer

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
    ComposerAttachmentStrip(draft.attachments, imageLoader, enabled && !draft.inputBlocked) { files ->
        onChange(draft.copy(attachments = files))
    }
    ComposerActivityNotice(draft.activity, error)
}
