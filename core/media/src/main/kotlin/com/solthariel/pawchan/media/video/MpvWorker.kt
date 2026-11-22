package com.solthariel.pawchan.media.video

import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

/** Owns the serialized mpv thread and reports failed asynchronous operations to its owner. */
internal class MpvWorker(
    private val onFailure: (Int, String, Throwable) -> Unit,
) {
    private val executor = Executors.newSingleThreadScheduledExecutor()

    fun execute(
        token: Int,
        operation: String,
        action: () -> Unit,
    ) {
        executor.execute(task(token, operation, action))
    }

    fun schedule(
        token: Int,
        operation: String,
        delayMillis: Long,
        action: () -> Unit,
    ) {
        executor.schedule(task(token, operation, action), delayMillis, TimeUnit.MILLISECONDS)
    }

    fun shutdown() = executor.shutdown()

    private fun task(
        token: Int,
        operation: String,
        action: () -> Unit,
    ): FutureTask<Unit> =
        object : FutureTask<Unit>(Callable { action() }) {
            override fun done() {
                try {
                    get()
                } catch (failure: ExecutionException) {
                    val cause = failure.cause ?: failure
                    when (cause) {
                        is Exception, is LinkageError -> onFailure(token, operation, cause)
                        else -> throw cause
                    }
                } catch (_: CancellationException) {
                    // Cancellation is not a playback failure.
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    onFailure(token, operation, interrupted)
                }
            }
        }
}
