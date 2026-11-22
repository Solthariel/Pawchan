package com.solthariel.pawchan.media.video

/** The viewer retains its surface when the platform rejects the PiP request. */
internal fun commitPipTransfer(
    accepted: Boolean,
    transfer: () -> Unit,
): Boolean {
    if (!accepted) return false
    transfer()
    return true
}
