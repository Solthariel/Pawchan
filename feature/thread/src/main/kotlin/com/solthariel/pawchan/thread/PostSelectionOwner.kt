package com.solthariel.pawchan.thread

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** One selection per window, owned by its composition, never by a process singleton. */
internal class PostSelectionOwner {
    private var token: Any? = null
    private var bounds: (() -> Rect)? = null
    private var clear: (() -> Unit)? = null

    fun select(
        owner: Any,
        area: () -> Rect,
        onClear: () -> Unit,
    ) {
        if (token !== owner) clear?.invoke()
        token = owner
        bounds = area
        clear = onClear
    }

    fun release(owner: Any) {
        if (token === owner) {
            token = null
            bounds = null
            clear = null
        }
    }

    fun outsideDown(position: Offset) {
        val area = bounds?.invoke() ?: return
        if (!area.contains(position)) {
            val action = clear
            token = null
            bounds = null
            clear = null
            action?.invoke()
        }
    }
}

internal val LocalPostSelectionOwner = staticCompositionLocalOf<PostSelectionOwner?> { null }

@Composable
fun PostSelectionHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val owner = remember { PostSelectionOwner() }
    CompositionLocalProvider(LocalPostSelectionOwner provides owner) {
        Box(
            modifier.pointerInput(owner) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    owner.outsideDown(down.position)
                }
            },
        ) { content() }
    }
}
