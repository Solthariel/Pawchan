package com.solthariel.pawchan.thread

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity

/** LazyColumn consumes scrolling first. Only a finger drag beyond its edge moves the window. */
@Composable
internal fun Modifier.previewSwipeModifier(
    state: PreviewSwipeState,
    pop: () -> Unit,
    clear: () -> Unit,
): Modifier {
    val density = LocalDensity.current.density
    val currentPop = rememberUpdatedState(pop)
    val currentClear = rememberUpdatedState(clear)
    val connection =
        remember(state, density) {
            object : NestedScrollConnection {
                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset =
                    if (source == NestedScrollSource.UserInput && state.offset != 0f) {
                        val delta =
                            if (state.offset * available.y < 0) {
                                available.y.coerceIn(-kotlin.math.abs(state.offset), kotlin.math.abs(state.offset))
                            } else {
                                available.y
                            }
                        Offset(0f, state.drag(delta))
                    } else {
                        Offset.Zero
                    }

                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero
                    return Offset(0f, state.drag(available.y))
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (state.offset == 0f) return Velocity.Zero
                    state.release(available.y, density, currentPop.value, currentClear.value)
                    return available
                }
            }
        }
    return nestedScroll(connection).previewGestureLifecycle(state)
}

/** Observe completion without consuming taps, text selection, or list scrolling. */
private fun Modifier.previewGestureLifecycle(state: PreviewSwipeState): Modifier =
    pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            state.beginGesture()
            var completed = false
            try {
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    completed = event.changes.none { it.pressed }
                    if (completed) state.endGesture()
                } while (!completed)
            } finally {
                if (!completed) state.cancelGesture()
            }
        }
    }
