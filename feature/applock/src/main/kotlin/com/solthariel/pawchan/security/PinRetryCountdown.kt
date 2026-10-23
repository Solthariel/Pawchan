package com.solthariel.pawchan.security

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owned by the lock ViewModel, not by a recomposed keypad. */
internal class PinRetryCountdown(
    private val scope: CoroutineScope,
    private val nowMillis: () -> Long,
    private val publish: (Int) -> Unit,
) {
    private var job: Job? = null

    fun start(remainingMillis: Long) {
        cancel()
        val deadline = nowMillis() + remainingMillis
        job =
            scope.launch {
                var remaining = remainingMillis
                while (remaining > 0) {
                    publish(((remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt())
                    delay(minOf(remaining, MILLIS_PER_SECOND))
                    remaining = (deadline - nowMillis()).coerceAtLeast(0)
                }
                publish(0)
            }
    }

    fun cancel() {
        job?.cancel()
        job = null
        publish(0)
    }
}

private const val MILLIS_PER_SECOND = 1_000L
