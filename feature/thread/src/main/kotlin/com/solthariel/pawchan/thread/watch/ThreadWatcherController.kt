package com.solthariel.pawchan.thread.watch

import com.solthariel.pawchan.thread.ThreadError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal data class ThreadWatcherUiState(
    val enabled: Boolean = false,
    val active: Boolean = false,
    val checking: Boolean = false,
    val newPosts: Int = 0,
    val error: ThreadError? = null,
    val stopReason: ThreadWatcherStopReason? = null,
)

internal enum class ThreadWatcherStopReason {
    CLOSED,
    DELETED,
}

internal data class ThreadWatcherPoll(
    val newPosts: Int,
    val error: ThreadError? = null,
    val stopReason: ThreadWatcherStopReason? = null,
)

internal class ThreadWatcherController(
    private val scope: CoroutineScope,
    private var intervalMillis: Long = DEFAULT_WATCH_INTERVAL_MILLIS,
    private val poll: suspend () -> ThreadWatcherPoll?,
) {
    private val mutableState = MutableStateFlow(ThreadWatcherUiState())
    private var job: Job? = null
    private var automatic = false

    fun configureAutomatic(
        enabled: Boolean,
        interval: Long,
    ) {
        automatic = enabled
        intervalMillis = interval.coerceAtLeast(MIN_POLL_INTERVAL_MILLIS)
        restartPolling()
    }

    val state: StateFlow<ThreadWatcherUiState> = mutableState.asStateFlow()

    fun toggle() {
        setEnabled(!mutableState.value.enabled)
    }

    fun setEnabled(enabled: Boolean) {
        val active = mutableState.value.active
        mutableState.value = ThreadWatcherUiState(enabled = enabled, active = active)
        restartPolling()
    }

    fun setActive(active: Boolean) {
        if (active == mutableState.value.active) {
            return
        }
        mutableState.value = mutableState.value.copy(active = active, checking = false)
        restartPolling()
    }

    private fun restartPolling() {
        job?.cancel()
        job = null
        if ((!mutableState.value.enabled && !automatic) || !mutableState.value.active) {
            return
        }
        job =
            scope.launch {
                while (isActive) {
                    delay(intervalMillis)
                    mutableState.value = mutableState.value.copy(checking = true)
                    val result = poll()
                    applyResult(result)
                    if (result?.stopReason != null) {
                        break
                    }
                }
            }
    }

    private fun applyResult(result: ThreadWatcherPoll?) {
        val current = mutableState.value
        mutableState.value =
            if (result == null) {
                current.copy(checking = false)
            } else {
                current.copy(
                    enabled = current.enabled && result.stopReason == null,
                    checking = false,
                    newPosts = current.newPosts + result.newPosts,
                    error = result.error,
                    stopReason = result.stopReason,
                )
            }
    }

    companion object {
        const val DEFAULT_WATCH_INTERVAL_MILLIS = 30_000L
    }
}

private const val MIN_POLL_INTERVAL_MILLIS = 15_000L
