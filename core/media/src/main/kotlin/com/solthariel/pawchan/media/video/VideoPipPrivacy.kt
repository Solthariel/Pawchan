package com.solthariel.pawchan.media.video

/** Revocation is terminal for an external window; stale layout/control events cannot revive it. */
internal class VideoPipPrivacy(
    private val allowed: () -> Boolean,
    private val revoke: () -> Unit,
) {
    private var revoked = false

    fun allowed(): Boolean = !revoked && allowed.invoke()

    fun revoke() {
        if (revoked) return
        revoked = true
        revoke.invoke()
    }
}
