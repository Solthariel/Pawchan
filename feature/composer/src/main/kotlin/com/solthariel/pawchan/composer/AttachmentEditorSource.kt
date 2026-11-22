package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Read image eligibility and EXIF-oriented bounds away from the UI thread. */
@Composable
internal fun rememberAttachmentEditorDimensions(file: ComposerAttachment): ReencodingDimensions? {
    LocalAttachmentViewerSource.current?.takeIf { it.source === file.source.content }?.let { return it.dimensions }
    val dimensions by produceState<ReencodingDimensions?>(null, file.id, file.source.content) {
        value = withContext(Dispatchers.IO) { attachmentEditorDimensions(file) }
    }
    return dimensions
}

internal fun attachmentEditorDimensions(file: ComposerAttachment): ReencodingDimensions? =
    if (!isEditableStillImage(file.source.content)) {
        null
    } else {
        readReencodingSource(file.source.content)?.let {
            ReencodingDimensions(it.width, it.height).oriented(it.orientation)
        }
    }
