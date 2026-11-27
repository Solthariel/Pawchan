package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import coil3.ImageLoader
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun AttachmentEditorCanvas(
    file: ComposerAttachment,
    imageLoader: ImageLoader,
    draft: AttachmentViewerDraft,
    dimensions: ReencodingDimensions?,
    actions: AttachmentEditorActions,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current

    val tab = if (dimensions == null) AttachmentEditorTab.FILE else draft.tab
    if (dimensions == null) {
        ComposerViewerMedia(file, imageLoader)
    } else {
        val original = rememberEditorPreview(file.source.content, DraftImageEdit())
        val size = requireNotNull(dimensions)
        val config = attachmentEditorCanvasConfig(draft, size, tab).copy(sourceSize = size)
        Box(modifier) {
            ImageEditorCanvas(
                file.source.content,
                draft.editor,
                config,
                actions.onLimit,
                Modifier.fillMaxSize(),
                options =
                    ImageCanvasOptions(
                        enabled = actions.enabled && !draft.comparing,
                        comparisonBitmap = original?.bitmap.takeIf { draft.comparing },
                    ),
            )
            if (draft.comparing && original?.bitmap == null && original != null) {
                Text(res.getString(L.ui_failed_to_load_original), color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}

private fun attachmentEditorCanvasConfig(
    draft: AttachmentViewerDraft,
    dimensions: ReencodingDimensions,
    tab: AttachmentEditorTab,
): ImageEditorCanvasConfig =
    when (tab) {
        AttachmentEditorTab.CROP -> {
            ImageEditorCanvasConfig(
                ImageEditorCanvasMode.CROP,
                cropAspect =
                    draft.cropPreset.aspect(draft.editor.edit, dimensions.width, dimensions.height)?.let {
                        if (draft.cropPreset == ImageCropPreset.ORIGINAL &&
                            draft.cropOriginalInverted
                        ) {
                            1f / it
                        } else {
                            it
                        }
                    },
            )
        }

        AttachmentEditorTab.DRAW -> {
            draft.tools.drawingConfig(dimensions.width, dimensions.height)
        }

        AttachmentEditorTab.REDACT -> {
            draft.tools.redactionConfig(dimensions)
        }

        AttachmentEditorTab.FILE, AttachmentEditorTab.NAME, AttachmentEditorTab.ROTATE -> {
            ImageEditorCanvasConfig(ImageEditorCanvasMode.FILE)
        }
    }
