package com.solthariel.pawchan.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Ephemeral input: never saved, and discarded when the lock screen leaves the foreground. */
@Composable
internal fun rememberPinUnlockInput(state: AppLockState): PinUnlockInput {
    val digits =
        remember(
            state.pinInputEpoch,
            state.editor?.stage,
            state.busy,
            state.error,
            state.ready,
            state.retrySeconds > 0,
        ) {
            PinUnlockInput()
        }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, digits) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) digits.clear() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            digits.clear()
        }
    }
    return digits
}
