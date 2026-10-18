package com.solthariel.pawchan.domain.security

/** A retained owner supplies the monotonic clock; process startup is handled as locked by its host. */
class AppLockTiming(
    private val nowMillis: () -> Long,
) {
    private var backgroundAt: Long? = null

    fun background() {
        if (backgroundAt == null) backgroundAt = nowMillis()
    }

    fun shouldLockOnForeground(delay: AppLockDelay = AppLockDelay.MINUTE_1): Boolean {
        val since = backgroundAt
        backgroundAt = null
        return since != null && nowMillis() - since >= delay.millis
    }
}
