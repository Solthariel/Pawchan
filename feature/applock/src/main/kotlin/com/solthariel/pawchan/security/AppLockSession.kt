package com.solthariel.pawchan.security

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.domain.security.AppLockDelay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Activity-owned, survives rotation; a new process is always locked. */
class AppLockSession internal constructor(
    private val store: AppLockStorage,
    nowMillis: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {
    constructor(context: Context) : this(AppLockStore(context.applicationContext))

    private var activeOperation: Job? = null
    private val mutable = MutableStateFlow(AppLockState())
    internal val state = mutable.asStateFlow()
    val accessChanges: Flow<Boolean> = state.map { it.ready && !it.locked }.distinctUntilChanged()

    fun isAccessAllowed(): Boolean = state.value.ready && !state.value.locked

    internal val fingerprintAvailable get() = store.fingerprintAvailable
    val lifecycle: AppLockLifecycle =
        AppLockLifecycle(viewModelScope, nowMillis, { state.value }, ::lock)
    private val retry =
        PinRetryCountdown(viewModelScope, nowMillis) {
            mutable.value = state.value.copy(pinInput = state.value.pinInput.copy(retrySeconds = it))
        }

    init {
        operation {
            val config = store.read()
            mutable.value = state.value.copy(ready = true, configuration = config, locked = config.enabled)
            retry.start(store.retryRemainingMillis())
        }
    }

    internal fun edit(action: LockAction) {
        if (!state.value.canEdit) return
        val stage = if (action == LockAction.ENABLE) PinStage.NEW else PinStage.CURRENT
        mutable.value = state.value.copy(editor = LockEditor(action, stage), error = null)
    }

    internal fun setDelay(delay: AppLockDelay) {
        val current = state.value
        if (!current.canEdit || !current.configuration.enabled) return
        operation {
            store.setDelay(delay)
            mutable.value = state.value.copy(configuration = store.read(), error = null)
        }
    }

    internal fun cancelEdit() {
        if (!state.value.busy) {
            mutable.value = state.value.copy(editor = null, error = null)
        }
    }

    internal fun enterPin(pin: String) {
        if (state.value.busy || state.value.retrySeconds > 0 || state.value.biometricSetupReady) return
        if (pin.length != PIN_LENGTH || pin.any { it !in '0'..'9' }) return
        operation {
            val editor = state.value.editor
            if (editor == null || editor.stage == PinStage.CURRENT) {
                verifyCurrent(pin, editor)
            } else if (editor.stage == PinStage.NEW) {
                mutable.value =
                    state.value.copy(
                        editor =
                            editor.copy(
                                stage = PinStage.CONFIRM,
                                first = pin,
                            ),
                        error = null,
                    )
            } else if (pin == editor.first) {
                store.setPin(pin)
                mutable.value =
                    state.value.copy(
                        configuration = store.read(),
                        editor = null,
                        locked = false,
                        error = null,
                    )
            } else {
                mutable.value =
                    state.value.copy(
                        editor =
                            editor.copy(
                                stage = PinStage.NEW,
                                first = "",
                            ),
                        error = "PIN-коды не совпадают",
                    )
            }
        }
    }

    private suspend fun verifyCurrent(
        pin: String,
        editor: LockEditor?,
    ) {
        when (store.verify(pin)) {
            PinVerification.INVALID -> {
                mutable.value = state.value.copy(error = "Неверный PIN-код")
            }

            PinVerification.WAIT -> {
                retry.start(store.retryRemainingMillis())
                mutable.value = state.value.copy(error = "Неверный PIN-код")
            }

            PinVerification.VALID -> {
                afterVerification(editor)
            }
        }
    }

    private suspend fun afterVerification(editor: LockEditor?) {
        when (editor?.action) {
            null -> {
                mutable.value = state.value.copy(locked = false, error = null)
            }

            LockAction.CHANGE -> {
                mutable.value = state.value.copy(editor = editor.copy(stage = PinStage.NEW), error = null)
            }

            LockAction.DISABLE -> {
                store.clear()
                mutable.value = state.value.copy(configuration = store.read(), editor = null)
            }

            LockAction.BIOMETRIC_ENABLE -> {
                mutable.value = state.value.copy(editor = editor.copy(stage = PinStage.BIOMETRIC), error = null)
            }

            LockAction.BIOMETRIC_DISABLE -> {
                store.setBiometric(false)
                mutable.value = state.value.copy(configuration = store.read(), editor = null, error = null)
            }

            LockAction.ENABLE -> {
                Unit
            }
        }
    }

    internal fun biometric(
        activity: Activity,
        setup: Boolean = false,
    ) {
        if (state.value.busy || !fingerprintAvailable) return
        val allowed =
            if (setup) state.value.biometricSetupReady else state.value.locked && state.value.configuration.biometric
        if (!allowed) return
        operation {
            val cipher = store.biometricCipher(recreate = setup)
            val authenticated = authenticateFingerprint(activity, cipher)
            if (authenticated == null) {
                mutable.value = state.value.copy(error = "Введите PIN или используйте отпечаток")
            } else {
                authenticated.doFinal(byteArrayOf(1))
                store.resetAttempts()
                retry.cancel()
                if (setup) store.setBiometric(true)
                mutable.value =
                    state.value.copy(
                        configuration = store.read(),
                        locked = false,
                        editor = null,
                        error = null,
                    )
            }
        }
    }

    private fun operation(block: suspend () -> Unit) {
        if (state.value.busy) return
        val epoch = state.value.pinInputEpoch + 1
        mutable.value = state.value.copy(busy = true, pinInput = state.value.pinInput.copy(epoch = epoch))
        activeOperation =
            viewModelScope.launch {
                try {
                    block()
                } catch (
                    cancelled: CancellationException,
                ) {
                    throw cancelled
                } catch (
                    _: Exception,
                ) {
                    if (state.value.pinInputEpoch == epoch) {
                        mutable.value =
                            state.value.copy(
                                error = "Не удалось прочитать или сохранить настройки блокировки. Повторите попытку.",
                            )
                    }
                } finally {
                    if (state.value.pinInputEpoch == epoch) mutable.value = state.value.copy(busy = false)
                }
            }
    }

    private fun lock() {
        mutable.value =
            state.value.copy(
                locked = true,
                busy = false,
                editor = null,
                error = null,
                pinInput = state.value.pinInput.copy(epoch = state.value.pinInputEpoch + 1),
            )
        activeOperation?.cancel()
        activeOperation = null
    }

    override fun onCleared() {
        lifecycle.close()
    }
}
