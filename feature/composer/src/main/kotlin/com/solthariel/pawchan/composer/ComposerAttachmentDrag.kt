package com.solthariel.pawchan.composer

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
internal fun Modifier.composerAttachmentDragModifier(
    state: ComposerAttachmentDragState,
    grid: ComposerDropGrid,
    files: List<ComposerAttachment>,
    enabled: Boolean,
    onChange: (List<ComposerAttachment>) -> Unit,
): Modifier {
    val currentFiles by rememberUpdatedState(files)
    val currentGrid by rememberUpdatedState(grid)
    val changeFiles by rememberUpdatedState(onChange)
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(enabled, files.map { it.id }) {
        if (!enabled || (state.id != null && !state.matches(files))) state.finish()
    }
    if (!enabled || files.size < 2) return this
    return this.pointerInput(enabled) {
        detectDragGesturesAfterLongPress(
            onDragStart = { point ->
                if (state.begin(currentFiles, currentGrid, point)) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            },
            onDrag = { change, _ ->
                if (state.id != null) {
                    change.consume()
                    state.move(currentGrid, change.position)
                }
            },
            onDragCancel = state::finish,
            onDragEnd = {
                val result = state.arrange(currentFiles)
                state.finish()
                if (result.map { it.id } != currentFiles.map { it.id }) changeFiles(result)
            },
        )
    }
}
