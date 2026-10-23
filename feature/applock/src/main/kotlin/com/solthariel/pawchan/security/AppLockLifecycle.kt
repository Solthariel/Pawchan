package com.solthariel.pawchan.security

import com.solthariel.pawchan.domain.security.AppLockTiming
import kotlinx.coroutines.CoroutineScope

/** Retained activity owner: only a matching picker result can exempt one round trip. */
class AppLockLifecycle internal constructor(
    scope: CoroutineScope,
    nowMillis: () -> Long,
    private val state: () -> AppLockState,
    onLock: () -> Unit,
) {
    private val timing = AppLockTiming(nowMillis)
    private val picker = PickerLockGate(nowMillis)
    private val timer = AppLockBackgroundTimer(scope, onLock)
    val externalSelection: AppLockExternalSelection =
        object : AppLockExternalSelection {
            override fun begin() {
                if (state().ready && !state().locked) picker.begin()
            }

            override fun returned() {
                picker.returned()
            }

            override fun cancel() {
                picker.revoke()
            }
        }

    fun background() {
        timing.background()
        if (!state().configuration.enabled) return
        timer.start(if (picker.deferBackground()) PICKER_LEASE_MILLIS else state().configuration.delay.millis)
    }

    fun foreground() {
        timer.cancel()
        if (picker.hasDeferredBackground()) return
        if (timing.shouldLockOnForeground(state().configuration.delay) && state().configuration.enabled) timer.lock()
    }

    fun finishForeground() {
        val deferred = picker.hasDeferredBackground()
        val allowed = picker.finish()
        if (!deferred) return
        timing.shouldLockOnForeground()
        if (!allowed && state().configuration.enabled) timer.lock()
    }

    fun screenOff() {
        picker.revoke()
        if (state().configuration.enabled) timer.lock()
    }

    internal fun close() {
        picker.revoke()
        timer.lock()
    }
}
