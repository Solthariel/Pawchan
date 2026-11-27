package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.feature.composer.R.string as L

/** Preview and hit targets share the same fitted frame and viewport transform. */
@Composable
internal fun ImageEditorCanvas(
    source: ByteArray,
    state: ImageEditorState,
    config: ImageEditorCanvasConfig,
    onLimit: () -> Unit,
    modifier: Modifier = Modifier,
    options: ImageCanvasOptions = ImageCanvasOptions(),
) {
    var viewport by remember(source) { mutableStateOf(ImageViewport()) }
    val preview = rememberCanvasPreview(source, state, config)
    val frame =
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("attachment-image-canvas")
            .clipToBounds()
            .imageViewportInput(options.enabled, config.mode) { viewport = viewport.transform(it) }
    BoxWithConstraints(frame, contentAlignment = Alignment.Center) {
        if (preview.dimensions == null) {
            AttachmentPreviewPlaceholder(preview.failed)
        } else {
            ImageEditorFittedFrame(state, config, onLimit, preview, viewport, options)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.ImageEditorFittedFrame(
    state: ImageEditorState,
    config: ImageEditorCanvasConfig,
    onLimit: () -> Unit,
    preview: CanvasPreview,
    viewport: ImageViewport,
    options: ImageCanvasOptions,
) {
    val res = LocalResources.current

    val dimensions = requireNotNull(preview.dimensions)
    val cropPadding = 24.dp
    val ratio = dimensions.width.toFloat() / dimensions.height
    val availableWidth = (maxWidth - cropPadding * 2).coerceAtLeast(0.dp)
    val availableHeight = (maxHeight - cropPadding * 2).coerceAtLeast(0.dp)
    val frameWidth = minOf(availableWidth, availableHeight * ratio)
    val frameHeight = frameWidth / ratio
    val width by animateDpAsState(frameWidth, tween(FRAME_TRANSITION_MS), label = "editor-frame-width")
    val height by animateDpAsState(frameHeight, tween(FRAME_TRANSITION_MS), label = "editor-frame-height")
    val frameStable = width == frameWidth && height == frameHeight
    Box(
        Modifier.size(width + cropPadding * 2, height + cropPadding * 2).graphicsLayer {
            scaleX = viewport.scale
            scaleY = viewport.scale
            translationX = viewport.x
            translationY = viewport.y
        },
    ) {
        Box(
            Modifier
                .size(width, height)
                .align(Alignment.Center)
                .testTag("editor-image-frame"),
        ) {
            if (options.comparisonBitmap != null) {
                Image(
                    options.comparisonBitmap.asImageBitmap(),
                    res.getString(L.ui_original_64ea),
                    Modifier.fillMaxSize().testTag("editor-comparison-image"),
                    contentScale = ContentScale.Fit,
                )
            } else {
                CanvasImageLayers(
                    state,
                    config,
                    preview.visible,
                    onLimit,
                    ImageCanvasPresentation(
                        preview.displayEdit,
                        preview.pendingMarks,
                        preview.failed,
                        options.enabled && frameStable && preview.visible != null,
                        preview.transform,
                    ),
                )
            }
        }
        if (options.comparisonBitmap == null) {
            EditorCropLayer(
                state,
                config,
                dimensions,
                options.enabled && preview.rendered && frameStable,
                cropPadding,
            )
        }
    }
}

@Composable
private fun EditorCropLayer(
    state: ImageEditorState,
    config: ImageEditorCanvasConfig,
    dimensions: IntSize,
    ready: Boolean,
    padding: androidx.compose.ui.unit.Dp,
) {
    if (config.mode != ImageEditorCanvasMode.CROP || !ready) return
    val rotated = state.edit.quarterTurns % 2 != 0
    val width = if (rotated) dimensions.height else dimensions.width
    val height = if (rotated) dimensions.width else dimensions.height
    val geometry = ImageCropGeometry(width, height, config.cropAspect, padding)
    ImagePaddedCropOverlay(state.edit, geometry, state.cropActions(), Modifier.fillMaxSize())
}

private data class CanvasPreview(
    val displayEdit: com.solthariel.pawchan.domain.drafts.DraftImageEdit,
    val pendingMarks: List<com.solthariel.pawchan.domain.drafts.DraftImageMark>?,
    val transform: ImagePreviewTransform?,
    val visible: Bitmap?,
    val dimensions: IntSize?,
    val rendered: Boolean,
    val failed: Boolean,
)

@Composable
private fun rememberCanvasPreview(
    source: ByteArray,
    state: ImageEditorState,
    config: ImageEditorCanvasConfig,
): CanvasPreview {
    val displayEdit = imageEditorDisplayEdit(state.edit)
    val liveMark = state.inGesture && state.gestureStart != null && config.markTool != null
    val previewEdit = if (liveMark) requireNotNull(state.gestureStart).copy(crop = DraftCrop()) else displayEdit
    val frame = rememberEditorPreview(source, previewEdit)
    val rendered = frame?.bitmap.takeIf { frame?.edit == previewEdit }
    val pending =
        frame?.let {
            imagePreviewContinuation(it.edit, displayEdit) ?: imagePreviewFallback(it.edit, displayEdit)
        }
    val transform =
        if (rendered ==
            null
        ) {
            frame?.let { imagePreviewFallbackTransform(it.edit, previewEdit) }
        } else {
            null
        }
    val visible = rendered ?: frame?.bitmap.takeIf { pending != null || transform != null }
    val visibleSize =
        visible?.let {
            if (transform?.quarterTurns?.rem(2) ==
                1
            ) {
                IntSize(it.height, it.width)
            } else {
                IntSize(it.width, it.height)
            }
        }
    return CanvasPreview(
        displayEdit,
        pending,
        transform,
        visible,
        rememberPreviewSize(source, visibleSize),
        rendered != null,
        frame?.bitmap == null && frame?.edit == previewEdit,
    )
}

/** Preserve only geometry while a new mode renders; never expose stale pixels or hit targets. */
@Composable
private fun rememberPreviewSize(
    source: ByteArray,
    rendered: IntSize?,
): IntSize? {
    var lastSize by remember(source) { mutableStateOf<IntSize?>(null) }
    val current = rendered
    if (current != null) SideEffect { lastSize = current }
    return current ?: lastSize
}

private data class ImageCanvasPresentation(
    val displayEdit: com.solthariel.pawchan.domain.drafts.DraftImageEdit,
    val pendingMarks: List<com.solthariel.pawchan.domain.drafts.DraftImageMark>?,
    val failed: Boolean,
    val interactive: Boolean,
    val transform: ImagePreviewTransform?,
)

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CanvasImageLayers(
    state: ImageEditorState,
    config: ImageEditorCanvasConfig,
    visible: Bitmap?,
    onLimit: () -> Unit,
    presentation: ImageCanvasPresentation,
) {
    val res = LocalResources.current

    if (visible != null) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val turns = presentation.transform?.quarterTurns ?: 0
            val imageSize =
                if (turns % 2 == 1) Modifier.requiredSize(maxHeight, maxWidth) else Modifier.fillMaxSize()
            Image(
                visible.asImageBitmap(),
                null,
                imageSize.graphicsLayer {
                    rotationZ = turns * QUARTER_TURN_DEGREES
                    scaleX = if (presentation.transform?.mirrored == true) -1f else 1f
                },
                contentScale = ContentScale.FillBounds,
            )
        }
        val sourceSize =
            config.sourceSize ?: if (presentation.displayEdit.quarterTurns % 2 == 0) {
                ReencodingDimensions(visible.width, visible.height)
            } else {
                ReencodingDimensions(visible.height, visible.width)
            }
        presentation.pendingMarks?.forEach { mark ->
            ImagePendingMarkOverlay(presentation.displayEdit, mark, sourceSize, Modifier.fillMaxSize())
        }
        if (config.mode != ImageEditorCanvasMode.CROP) ImageCropPreviewMask(state.edit, Modifier.fillMaxSize())
    }
    if (presentation.failed) {
        Text(
            res.getString(L.ui_failed_to_process_preview),
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
    }
    if (presentation.interactive) ImageEditorCanvasOverlay(state, config, onLimit)
}

@Composable
private fun ImageEditorCanvasOverlay(
    state: ImageEditorState,
    config: ImageEditorCanvasConfig,
    onLimit: () -> Unit,
) {
    when (config.mode) {
        ImageEditorCanvasMode.CROP -> {
            Unit // The crop overlay includes the padding around the fitted image.
        }

        ImageEditorCanvasMode.MARK -> {
            config.markTool?.let { tool ->
                ImageMarkOverlay(state.edit, tool, state.markActions(onLimit), Modifier.fillMaxSize())
            }
        }

        ImageEditorCanvasMode.ERASE -> {
            val eraser = requireNotNull(config.eraser)
            ImageEraserOverlay(
                state.edit,
                eraser.width,
                eraser.height,
                state.markActions(onLimit),
                Modifier.fillMaxSize(),
                eraser.radius,
            )
        }

        ImageEditorCanvasMode.FILE -> {
            Unit
        }
    }
}

private const val FRAME_TRANSITION_MS = 180
private const val QUARTER_TURN_DEGREES = 90f
