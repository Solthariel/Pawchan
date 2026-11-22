package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/** Never reuse a previous or original preview when the edit recipe changes. */
@Composable
internal fun ComposerEditedPreview(
    source: ByteArray,
    edit: DraftImageEdit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val preview = rememberImageEditPreview(source, edit)
    ComposerEditedPreviewContent(preview, description, modifier)
}

@Composable
internal fun ComposerEditedPreviewContent(
    preview: EditedPreviewState,
    description: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        when (preview) {
            EditedPreviewState.Loading -> {
                AttachmentPreviewPlaceholder(failed = false)
            }

            EditedPreviewState.Failed -> {
                AttachmentPreviewPlaceholder(failed = true)
            }

            is EditedPreviewState.Ready -> {
                Image(
                    preview.bitmap.asImageBitmap(),
                    description,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
        }
    }
}

internal sealed interface EditedPreviewState {
    data object Loading : EditedPreviewState

    data object Failed : EditedPreviewState

    data class Ready(
        val bitmap: Bitmap,
    ) : EditedPreviewState
}

/** A new recipe always starts blank instead of reusing a stale processed result. */
@Composable
internal fun rememberImageEditPreview(
    source: ByteArray,
    edit: DraftImageEdit,
    render: suspend (ByteArray, DraftImageEdit) -> Bitmap? = ::safeImageEditPreview,
): EditedPreviewState =
    key(source, edit) {
        val preview by produceState<EditedPreviewState>(EditedPreviewState.Loading, render) {
            val bitmap = withContext(previewDispatcher) { render(source, edit) }
            value = if (bitmap == null) EditedPreviewState.Failed else EditedPreviewState.Ready(bitmap)
        }
        preview
    }

internal suspend fun safeImageEditPreview(
    source: ByteArray,
    edit: DraftImageEdit,
): Bitmap? =
    try {
        val bitmap = renderImageEditPreview(source, edit)
        try {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            bitmap
        } catch (error: kotlinx.coroutines.CancellationException) {
            bitmap.recycle()
            throw error
        }
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: IOException) {
        null
    }

internal val previewDispatcher = Dispatchers.Default.limitedParallelism(1)
