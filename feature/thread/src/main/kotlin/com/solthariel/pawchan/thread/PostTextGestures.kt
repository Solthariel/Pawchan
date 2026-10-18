package com.solthariel.pawchan.thread

import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

internal data class PostTextGestures(
    val onTap: (Offset) -> Unit,
    val onDoubleTap: (Offset) -> Unit,
    val onLongPress: (Offset) -> Unit,
    val onPress: (Offset) -> Unit = {},
    val onRelease: () -> Unit = {},
    val targetAt: (Offset) -> PostTextTarget? = { null },
    val textKey: Any? = null,
)

/** Link/spoiler release is immediate; only plain text participates in double-tap selection. */
internal fun Modifier.postTextGestures(
    key: Any,
    selecting: Boolean,
    actions: PostTextGestures,
): Modifier =
    if (selecting) {
        this
    } else {
        composed {
            val current by rememberUpdatedState(actions)
            pointerInput(key, actions.textKey) {
                detectPostTextGestures { current }
            }
        }
    }
