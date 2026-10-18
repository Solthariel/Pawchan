package com.solthariel.pawchan.security

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle

internal val LocalAppLock = staticCompositionLocalOf<AppLockViewModel?> { null }

@Composable
internal fun AppLockHost(
    activity: Activity,
    model: AppLockViewModel,
    content: @Composable () -> Unit,
) {
    val state by model.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.ready, state.locked, state.configuration.biometric) {
        if (state.ready && state.locked && state.configuration.biometric) model.biometric(activity)
    }
    CompositionLocalProvider(
        LocalAppLock provides model,
        com.solthariel.pawchan.media.video.LocalVideoPipAllowed provides !state.configuration.enabled,
    ) {
        Box(Modifier.fillMaxSize()) {
            // Retain navigation and drafts, but remove locked content from rendering and accessibility.
            val hidden = !state.ready || state.locked
            Box(if (hidden) Modifier.alpha(0f).clearAndSetSemantics { } else Modifier) { content() }
            if (hidden) LockedScreenDialog(activity, model, state)
        }
    }
}

@Composable
private fun LockedScreenDialog(
    activity: Activity,
    model: AppLockViewModel,
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
        MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
            Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    if (state.ready) {
                        PinPad("Введите PIN-код", state, model::enterPin)
                    } else {
                        Text(state.error ?: "Загрузка…")
                    }
                    if (state.configuration.biometric && model.fingerprintAvailable) {
                        TextButton(enabled = !state.busy, onClick = { model.biometric(activity) }) {
                            Text("Отпечаток пальца")
                        }
                    }
                }
            }
        }
    }
}
