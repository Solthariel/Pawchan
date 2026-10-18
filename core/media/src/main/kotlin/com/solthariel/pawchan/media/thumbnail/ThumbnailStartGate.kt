package com.solthariel.pawchan.media.thumbnail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** One non-blocking dwell timer per live URL task; scrolling never restarts it. */
internal class ThumbnailStartGate(
    scope: CoroutineScope,
) : AutoCloseable {
    private val mutableReady = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = mutableReady
    private val job =
        scope.launch {
            delay(THUMBNAIL_NETWORK_DWELL_MILLIS)
            mutableReady.value = true
        }

    override fun close() {
        mutableReady.value = false
        job.cancel()
    }
}

private const val THUMBNAIL_NETWORK_DWELL_MILLIS = 500L
