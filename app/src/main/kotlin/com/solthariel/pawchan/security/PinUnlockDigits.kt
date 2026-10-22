package com.solthariel.pawchan.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Ephemeral input: never saved, and discarded when the lock screen leaves the foreground. */
@Composable
internal fun rememberPinUnlockDigits(state: AppLockState): MutableState<String> {
    val digits = remember(state.busy, state.error, state.ready) { mutableStateOf("") }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, digits) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) digits.value = "" }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            digits.value = ""
        }
    }
    return digits
}
