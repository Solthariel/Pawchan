package com.solthariel.pawchan.security

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal val LocalAppLock = staticCompositionLocalOf<AppLockSession?> { null }

@Composable
fun AppLockScreenHost(
    activity: Activity,
    model: AppLockSession,
    content: @Composable (accessAllowed: Boolean) -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.ready, state.locked, state.configuration.biometric) {
        if (state.ready && state.locked && state.configuration.biometric) model.biometric(activity)
    }
    CompositionLocalProvider(
        LocalAppLock provides model,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Retain navigation and drafts, but remove locked content from rendering and accessibility.
            val hidden = !state.ready || state.locked
            Box(if (hidden) Modifier.alpha(0f).clearAndSetSemantics { } else Modifier) { content(!hidden) }
            if (hidden) LockedScreenDialog(activity, model, state)
        }
    }
}

@Composable
private fun LockedScreenDialog(
    activity: Activity,
    model: AppLockSession,
    state: AppLockState,
) {
    Dialog(
        onDismissRequest = {},
        properties =
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                securePolicy = SecureFlagPolicy.SecureOff,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
    ) {
        PinUnlockWindow()
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(Modifier.fillMaxSize()) {
                PinUnlockContent(state, model.fingerprintAvailable, model::enterPin) { model.biometric(activity) }
            }
        }
    }
}
