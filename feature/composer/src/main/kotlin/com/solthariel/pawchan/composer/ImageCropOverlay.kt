package com.solthariel.pawchan.composer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.ImageCanvasPoint
import com.solthariel.pawchan.domain.drafts.cropOnFullCanvas
import com.solthariel.pawchan.domain.drafts.resizeCrop

/** Modifier must match the fitted bounds of the full, rotated source image. */
@Composable
internal fun ImageCropOverlay(
    edit: DraftImageEdit,
    sourceWidth: Int,
    sourceHeight: Int,
    aspect: Float?,
    actions: ImageCropActions,
    modifier: Modifier = Modifier,
) {
    ImagePaddedCropOverlay(edit, ImageCropGeometry(sourceWidth, sourceHeight, aspect), actions, modifier)
}

internal data class ImageCropGeometry(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val aspect: Float?,
    val padding: Dp = 0.dp,
)

@Composable
internal fun ImagePaddedCropOverlay(
    edit: DraftImageEdit,
    geometry: ImageCropGeometry,
    actions: ImageCropActions,
    modifier: Modifier = Modifier,
) {
    val currentEdit = rememberUpdatedState(edit)
    val currentActions = rememberUpdatedState(actions)
    val touchRadius = with(LocalDensity.current) { CROP_TOUCH_RADIUS.dp.toPx() }
    val padding = with(LocalDensity.current) { geometry.padding.toPx() }
    val crop = edit.cropOnFullCanvas()
    Canvas(
        modifier
            .semantics { contentDescription = "Рамка обрезки" }
            .cropPointerInput(geometry, padding, touchRadius, currentEdit, currentActions),
    ) { inset(padding) { drawImageCropGuide(crop) } }
}

private fun Modifier.cropPointerInput(
    geometry: ImageCropGeometry,
    padding: Float,
    touchRadius: Float,
    currentEdit: State<DraftImageEdit>,
    currentActions: State<ImageCropActions>,
): Modifier =
    pointerInput(geometry, touchRadius) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val start = currentEdit.value
            val bounds = start.cropOnFullCanvas()
            val width = size.width.toFloat() - 2 * padding
            val height = size.height.toFloat() - 2 * padding
            if (width <= 0f || height <= 0f) return@awaitEachGesture
            val handle =
                cropHandleAt(
                    bounds,
                    width,
                    height,
                    down.position.x - padding,
                    down.position.y - padding,
                    touchRadius,
                )
            if (handle != null) {
                val anchor = cropHandlePosition(bounds, handle)
                currentActions.value.onStart()
                var complete = false
                try {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (event.changes.count { it.pressed } > 1 || change == null || !change.pressed) {
                            complete = event.changes.count { it.pressed } <= 1
                            break
                        }
                        val delta = change.position - down.position
                        if (delta != Offset.Zero) {
                            val x = anchor.x + delta.x / width
                            val y = anchor.y + delta.y / height
                            val point = ImageCanvasPoint(x, y)
                            val resized =
                                start.resizeCrop(
                                    handle,
                                    point,
                                    geometry.sourceWidth,
                                    geometry.sourceHeight,
                                    geometry.aspect,
                                )
                            currentActions.value.onChange(resized)
                            change.consume()
                        }
                    }
                } finally {
                    if (complete) currentActions.value.onFinish() else currentActions.value.onCancel()
                }
            }
        }
    }

private const val CROP_TOUCH_RADIUS = 24
