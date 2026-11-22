package com.solthariel.pawchan.composer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit

/** A drag removes whole drawing marks as one undoable gesture. */
@Composable
internal fun ImageEraserOverlay(
    edit: DraftImageEdit,
    sourceWidth: Int,
    sourceHeight: Int,
    actions: ImageMarkActions,
    modifier: Modifier = Modifier,
    radius: Float = ERASER_RADIUS,
) {
    val currentEdit by rememberUpdatedState(edit)
    val currentActions by rememberUpdatedState(actions)
    Canvas(
        modifier.semantics { contentDescription = "Поле ластика" }.pointerInput(sourceWidth, sourceHeight, radius) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val width = size.width.toFloat()
                val height = size.height.toFloat()
                val invalidSource = sourceWidth <= 0 || sourceHeight <= 0
                val invalidFrame = width <= 0 || height <= 0
                if (invalidSource || invalidFrame) return@awaitEachGesture
                val gesture = ImageEraserGesture(currentEdit.copy(crop = DraftCrop()), sourceWidth, sourceHeight)
                currentActions.onStart()
                var complete = false
                try {
                    fun erase(position: Offset) {
                        val x = (position.x / width).coerceIn(0f, 1f)
                        val y = (position.y / height).coerceIn(0f, 1f)
                        currentActions.onChange(gesture.erase(x, y, radius).copy(crop = currentEdit.crop))
                    }
                    erase(down.position)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        val multiple = event.changes.count { it.pressed } > 1
                        if (multiple || change == null || !change.pressed) {
                            complete = !multiple && change != null
                            break
                        }
                        erase(change.position)
                        change.consume()
                    }
                } finally {
                    if (complete) currentActions.onFinish() else currentActions.onCancel()
                }
            }
        },
    ) {}
}

private const val ERASER_RADIUS = 0.025f
