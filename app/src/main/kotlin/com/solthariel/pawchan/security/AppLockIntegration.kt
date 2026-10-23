package com.solthariel.pawchan.security

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.solthariel.pawchan.composer.LocalComposerPickerHost
import com.solthariel.pawchan.media.video.LocalVideoPipAllowed
import com.solthariel.pawchan.media.video.LocalVideoPrivacyAccess

/** App-owned wiring; the lock feature never imports composer, media or settings UI. */
@Composable
internal fun AppLockHost(
    activity: Activity,
    session: AppLockSession,
    content: @Composable () -> Unit,
) {
    val picker = remember(session) { ComposerLockBinding(session.lifecycle.externalSelection) }
    val video = remember(session) { LockVideoAccess(session) }
    AppLockScreenHost(activity, session) { accessAllowed ->
        CompositionLocalProvider(
            LocalComposerPickerHost provides picker,
            LocalVideoPipAllowed provides accessAllowed,
            LocalVideoPrivacyAccess provides video,
        ) {
            content()
        }
    }
}

@Composable
internal fun AppLockSettings() {
    AppLockSettingsHost { state, fingerprintAvailable, onDelay, onEdit ->
        AppLockSettingsContent(state, fingerprintAvailable, onDelay, onEdit)
    }
}
