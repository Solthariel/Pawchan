package com.solthariel.pawchan.security

internal enum class PinUnlockAction { FINGERPRINT, CONFIRM }

internal fun pinUnlockAction(
    digits: String,
    biometricEnabled: Boolean,
): PinUnlockAction = if (digits.isEmpty() && biometricEnabled) PinUnlockAction.FINGERPRINT else PinUnlockAction.CONFIRM

internal fun pinUnlockActionEnabled(
    digits: String,
    action: PinUnlockAction,
    state: AppLockState,
): Boolean = state.ready && !state.busy && (action == PinUnlockAction.FINGERPRINT || digits.length == PIN_LENGTH)
