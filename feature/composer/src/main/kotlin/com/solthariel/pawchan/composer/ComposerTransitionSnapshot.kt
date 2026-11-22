package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Keep the last rendered state while a closing form fades out. */
@Composable
internal fun <T> rememberComposerTransitionSnapshot(
    visible: Boolean,
    state: T,
): T {
    var lastVisible by remember { mutableStateOf(state) }
    SideEffect {
        if (visible) lastVisible = state
    }
    return if (visible) state else lastVisible
}
