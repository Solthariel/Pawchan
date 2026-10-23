package com.solthariel.pawchan.security

import com.solthariel.pawchan.domain.security.AppLockDelay

/** Read-only settings projection. Never contains PIN digits or the editor's staged PIN. */
data class AppLockSettingsState(
    val enabled: Boolean = false,
    val biometric: Boolean = false,
    val delay: AppLockDelay = AppLockDelay.MINUTE_1,
    val editing: Boolean = false,
    val error: String? = null,
)

internal fun AppLockState.settingsProjection() =
    AppLockSettingsState(
        enabled = configuration.enabled,
        biometric = configuration.biometric,
        delay = configuration.delay,
        editing = editor != null,
        error = error,
    )
