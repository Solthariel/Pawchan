package com.solthariel.pawchan.security

enum class LockAction { ENABLE, CHANGE, DISABLE, BIOMETRIC_ENABLE, BIOMETRIC_DISABLE }

internal enum class PinStage { CURRENT, NEW, CONFIRM, BIOMETRIC }

internal data class LockEditor(
    val action: LockAction,
    val stage: PinStage,
    val first: String = "",
)

internal data class AppLockState(
    val ready: Boolean = false,
    val configuration: LockConfiguration = LockConfiguration(),
    val locked: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val editor: LockEditor? = null,
    val pinInput: PinInputState = PinInputState(),
) {
    val retrySeconds: Int get() = pinInput.retrySeconds
    val pinInputEpoch: Long get() = pinInput.epoch
    val biometricSetupReady: Boolean get() = editor?.stage == PinStage.BIOMETRIC
    val canEdit: Boolean get() = ready && !busy && !locked
}

/** One input attempt identity and its temporary retry restriction; never contains PIN digits. */
internal data class PinInputState(
    val epoch: Long = 0,
    val retrySeconds: Int = 0,
)
