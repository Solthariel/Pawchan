package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag

/** A full-viewport dialog needs its own outside hit target; the surface consumes inside taps. */
@Composable
internal fun DialogBackdrop(
    modifier: Modifier,
    enabled: Boolean,
    onDismiss: () -> Unit,
) {
    val dismiss by rememberUpdatedState(onDismiss)
    Box(
        modifier.testTag("dialog-backdrop").pointerInput(enabled) {
            detectTapGestures { if (enabled) dismiss() }
        },
    )
}
