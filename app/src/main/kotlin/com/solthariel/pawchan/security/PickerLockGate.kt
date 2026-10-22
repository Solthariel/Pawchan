package com.solthariel.pawchan.security

/** One app-launched picker round trip. Never grants access after a fresh process or screen off. */
internal class PickerLockGate(
    private val nowMillis: () -> Long,
) {
    private var started: Long? = null
    private var returned = false
    private var deferred = false

    fun begin() {
        started = nowMillis()
        returned = false
    }

    fun returned() {
        if (deferred && started != null) returned = true else revoke()
    }

    fun revoke() {
        started = null
        returned = false
        deferred = false
    }

    fun deferBackground(): Boolean {
        deferred = started != null && nowMillis() - requireNotNull(started) in 0..PICKER_LEASE_MILLIS
        return deferred
    }

    fun hasDeferredBackground(): Boolean = deferred

    fun finish(): Boolean {
        val allow = deferred && returned && started?.let { nowMillis() - it in 0..PICKER_LEASE_MILLIS } == true
        revoke()
        return allow
    }
}

internal const val PICKER_LEASE_MILLIS = 300_000L
