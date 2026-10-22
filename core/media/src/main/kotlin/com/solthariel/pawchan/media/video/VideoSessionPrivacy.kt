package com.solthariel.pawchan.media.video

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Follows the session through PiP parking; no Activity needs to remain alive to pause on lock. */
internal class VideoSessionPrivacy(
    private val scope: CoroutineScope,
    private val pause: () -> Unit,
) : AutoCloseable {
    private var binding: Job? = null
    private var access: VideoPrivacyAccess? = null

    fun bind(next: VideoPrivacyAccess?) {
        if (access === next && binding?.isActive == true) return
        binding?.cancel()
        access = next
        if (next?.isAllowed() == false) pause()
        binding =
            next?.let { policy ->
                scope.launch { policy.changes.collect { allowed -> if (!allowed) pause() } }
            }
    }

    override fun close() {
        binding?.cancel()
        binding = null
        access = null
    }
}
