package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager

@Composable
internal fun AttachmentNamePanel(
    file: ComposerAttachment,
    draft: AttachmentViewerDraft,
    enabled: Boolean,
) {
    val focus = LocalFocusManager.current
    AttachmentRenameControls(file, enabled, onDone = { focus.clearFocus() }) { draft.options = it.options }
}
