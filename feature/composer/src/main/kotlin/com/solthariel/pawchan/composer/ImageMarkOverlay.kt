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
import com.solthariel.pawchan.domain.drafts.DraftMarkGesture
import com.solthariel.pawchan.domain.drafts.DraftMarkTool
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.ImageCanvasPoint
import com.solthariel.pawchan.domain.drafts.canvasToSource

/** Place over the fitted output bitmap; one finger draws, two fingers are left for viewport gestures. */
@Composable
internal fun ImageMarkOverlay(
    edit: DraftImageEdit,
    tool: DraftMarkTool,
    actions: ImageMarkActions,
    modifier: Modifier = Modifier,
) {
    val currentEdit by rememberUpdatedState(edit)
    val currentActions by rememberUpdatedState(actions)
    Canvas(
        modifier
            .semantics { contentDescription = "Поле рисования" }
            .pointerInput(tool) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = currentEdit
                    val mapping = start.copy(crop = DraftCrop())
                    val width = size.width.toFloat()
                    val height = size.height.toFloat()
                    val point = imageMarkPoint(mapping, down.position, width, height)
                    if (point == null) return@awaitEachGesture
                    val gesture = DraftMarkGesture.begin(start, tool, point)
                    if (gesture == null) {
                        currentActions.onLimit()
                    } else {
                        currentActions.onStart()
                        var complete = false
                        try {
                            currentActions.onChange(gesture.snapshot())
                            var stopped = false
                            while (!stopped) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                val multiTouch = event.changes.count { it.pressed } > 1
                                if (multiTouch || change == null || !change.pressed) {
                                    complete = !multiTouch && change != null
                                    stopped = true
                                } else {
                                    val next = imageMarkPoint(mapping, change.position, width, height)
                                    if (next != null) {
                                        currentActions.onChange(gesture.update(next))
                                        change.consume()
                                        if (gesture.limitReached) {
                                            currentActions.onLimit()
                                            complete = true
                                            stopped = true
                                        }
                                    }
                                }
                            }
                        } finally {
                            if (complete) currentActions.onFinish() else currentActions.onCancel()
                        }
                    }
                }
            },
    ) {}
}

internal fun imageMarkPoint(
    edit: DraftImageEdit,
    position: Offset,
    width: Float,
    height: Float,
): DraftPoint? {
    val invalidPosition = !position.x.isFinite() || !position.y.isFinite()
    if (width <= 0f || height <= 0f || invalidPosition) return null
    val point = ImageCanvasPoint((position.x / width).coerceIn(0f, 1f), (position.y / height).coerceIn(0f, 1f))
    return edit.canvasToSource(point)
}
