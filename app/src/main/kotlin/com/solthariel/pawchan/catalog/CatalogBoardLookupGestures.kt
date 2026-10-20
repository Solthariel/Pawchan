package com.solthariel.pawchan.catalog

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.State
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope

internal suspend fun PointerInputScope.observeBoardLookupDismissal(
    lookup: State<CatalogBoardLookupBindings?>,
    panelBounds: State<Rect>,
    origin: State<Offset>,
    toolbarHeight: Float,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val active = lookup.value
        val insidePanel = panelBounds.value.contains(down.position + origin.value)
        if (active?.visible != true || down.position.y < toolbarHeight || insidePanel) return@awaitEachGesture
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            val dragging = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            if (dragging) {
                // Never consume the drag: the original list keeps the same gesture and position.
                active.onDismiss()
                return@awaitEachGesture
            }
            if (!change.pressed) {
                // Consume the first outside tap before child click handlers see its up event.
                change.consume()
                active.onDismiss()
            }
        } while (change.pressed)
    }
}
