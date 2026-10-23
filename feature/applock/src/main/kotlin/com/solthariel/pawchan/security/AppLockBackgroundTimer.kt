package com.solthariel.pawchan.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The retained lock owner revokes external media at the deadline, even while backgrounded. */
internal class AppLockBackgroundTimer(
    private val scope: CoroutineScope,
    private val onLock: () -> Unit,
) {
    private var pending: Job? = null

    fun start(timeout: Long) {
        cancel()
        if (timeout == 0L) {
            lock()
        } else {
            pending =
                scope.launch {
                    delay(timeout)
                    lock()
                }
        }
    }

    fun cancel() {
        pending?.cancel()
        pending = null
    }

    fun lock() {
        cancel()
        onLock()
    }
}
