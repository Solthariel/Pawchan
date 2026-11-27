package com.solthariel.pawchan.media.video

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException

/** Main-thread callbacks for a single session; this coordinator alone owns decoder attempts. */
internal interface VideoOpenHost {
    val requestedPlay: Boolean

    fun opening(attempt: VideoAttempt)

    fun applySound()

    fun keepPosition()

    fun received(
        type: VideoEvent,
        snapshot: VideoPlaybackStatus,
    )

    fun terminal(attempt: VideoAttempt)
}

internal class VideoSessionOpening(
    private val scope: CoroutineScope,
    private val engine: VideoEngineBackend,
    private val url: String,
    private val host: VideoOpenHost,
) : AutoCloseable {
    private var attempt = VideoAttempt()
    private var generation = 0
    private var job: Job? = null
    private var closed = false

    fun start() {
        if (closed) return
        val token = ++generation
        job?.cancel()
        engine.stop()
        host.opening(attempt)
        job =
            scope.launch {
                if (!current(token)) return@launch
                try {
                    open(token)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: IOException) {
                    if (current(token)) fail(VideoFailure.SOURCE)
                } catch (_: IllegalArgumentException) {
                    if (current(token)) fail(VideoFailure.SOURCE)
                }
            }
    }

    private fun current(token: Int): Boolean = !closed && token == generation

    private fun open(token: Int) {
        host.applySound()
        engine.open(url, attempt.software) { type, snapshot ->
            if (current(token)) {
                if (type == VideoEvent.ERROR) {
                    fail(snapshot.attempt.failure ?: VideoFailure.PLAYBACK)
                } else {
                    host.received(type, snapshot)
                }
            }
        }
        if (current(token) && !host.requestedPlay) engine.pause()
    }

    private fun fail(failure: VideoFailure) {
        val fallback = if (failure == VideoFailure.PLAYBACK) attempt.afterFailure() else null
        if (fallback != null) {
            host.keepPosition()
            attempt = fallback
            start()
        } else {
            generation++
            job?.cancel()
            engine.stop()
            host.terminal(attempt.copy(failure = failure))
        }
    }

    fun selectDecoder(
        software: Boolean,
        actualHardware: Boolean?,
    ) {
        val retryHardware = !software && !attempt.software && actualHardware == false
        if (!closed && (software != attempt.software || retryHardware)) {
            host.keepPosition()
            attempt = attempt.copy(software = software)
            start()
        }
    }

    override fun close() {
        closed = true
        generation++
        job?.cancel()
    }
}
