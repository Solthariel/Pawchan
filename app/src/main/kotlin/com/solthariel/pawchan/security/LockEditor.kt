package com.solthariel.pawchan.security

internal enum class LockAction { ENABLE, CHANGE, DISABLE, BIOMETRIC_ENABLE, BIOMETRIC_DISABLE }

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
    val retrySeconds: Int = 0,
) {
    val biometricSetupReady: Boolean get() = editor?.stage == PinStage.BIOMETRIC
    val canEdit: Boolean get() = ready && !busy && !locked
}
