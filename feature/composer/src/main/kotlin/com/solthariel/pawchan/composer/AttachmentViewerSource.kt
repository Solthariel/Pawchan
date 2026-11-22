package com.solthariel.pawchan.composer

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Owned by one opening of one attachment; no process-wide bitmap cache. */
internal class AttachmentViewerSource(
    val source: ByteArray,
    val dimensions: ReencodingDimensions?,
    val frames: ImagePreviewCache<ImageEditorPreviewFrame> = ImagePreviewCache(),
)

internal val LocalAttachmentViewerSource = staticCompositionLocalOf<AttachmentViewerSource?> { null }

/** Do not expose an empty window, or switch from Coil to the editor after opening. */
@Composable
internal fun PreparedAttachmentViewer(
    file: ComposerAttachment,
    onDismiss: () -> Unit,
    prepare: suspend (ComposerAttachment) -> AttachmentViewerSource = ::prepareAttachmentViewer,
    content: @Composable () -> Unit,
) {
    key(file.id, file.source.content) {
        val prepared by produceState<AttachmentViewerSource?>(null, prepare) { value = prepare(file) }
        val source = prepared
        if (source == null) {
            BackHandler(onBack = onDismiss)
            ComposerActivityNotice(ComposerDraftActivity.LOADING_FILES, null)
        } else {
            CompositionLocalProvider(LocalAttachmentViewerSource provides source, content = content)
        }
    }
}

internal suspend fun prepareAttachmentViewer(file: ComposerAttachment): AttachmentViewerSource {
    val dimensions = withContext(Dispatchers.IO) { attachmentEditorDimensions(file) }
    val result = AttachmentViewerSource(file.source.content, dimensions)
    if (dimensions != null) {
        val edit = (file.options.edit ?: DraftImageEdit()).copy(crop = DraftCrop())
        val bitmap = withContext(previewDispatcher) { safeImageEditPreview(file.source.content, edit) }
        // A failed result is ready too: show the error, not a permanently pending window.
        result.frames.put(edit, ImageEditorPreviewFrame(edit, bitmap))
    }
    return result
}
