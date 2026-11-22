package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import kotlinx.coroutines.withContext

internal data class ImageEditorPreviewFrame(
    val edit: DraftImageEdit,
    val bitmap: Bitmap?,
)

/** The cache belongs to one source/session and is bounded to four downsampled frames. */
@Composable
internal fun rememberEditorPreview(
    source: ByteArray,
    edit: DraftImageEdit,
): ImageEditorPreviewFrame? =
    key(source) {
        val prepared = LocalAttachmentViewerSource.current?.takeIf { it.source === source }
        val cache = remember(source, prepared) { prepared?.frames ?: ImagePreviewCache<ImageEditorPreviewFrame>() }
        val frame by produceState<ImageEditorPreviewFrame?>(cache[edit], edit) {
            val cached = cache[edit]
            if (cached != null) {
                value = cached
            } else {
                val bitmap = withContext(previewDispatcher) { safeImageEditPreview(source, edit) }
                val next = ImageEditorPreviewFrame(edit, bitmap)
                if (bitmap != null) cache.put(edit, next)
                value = next
            }
        }
        cache[edit] ?: frame
    }
