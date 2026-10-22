package com.solthariel.pawchan.media.video

import android.content.Context
import android.os.Handler
import android.widget.Toast
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.ffmpeg.PawPlayback
import com.solthariel.pawchan.ffmpeg.ProgressiveSource
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.concurrent.Executors

/** Native work and destruction never run on the Compose/main thread. */
internal class FfmpegVideoState(
    context: Context,
    private val client: OkHttpClient,
    private val directory: File,
) : AutoCloseable {
    private val main = Handler(context.mainLooper)
    private val worker = Executors.newSingleThreadExecutor()
    private val noticeContext = context.applicationContext
    var player: PawPlayback? = null
    private var source: ProgressiveSource? = null
    private var sourceKey: String? = null
    private var sourceSerial = 0
    val output = FfmpegRenderTarget()
    private var callback: (VideoEvent, VideoPlaybackStatus) -> Unit = { _, _ -> }

    @Volatile private var generation = 0

    @Volatile private var closed = false
    var initialized = false
    var requestedPlaying = true
    private var busy = true
    var ended = false
    var volume = VIDEO_VOLUME_MAX_PERCENT
    var muted = false
    var speed = VideoSpeed.NORMAL.milli
    private var initializingGeneration = -1

    private val stall = VideoStallDiagnostics()

    private val polling =
        object : Runnable {
            override fun run() {
                if (!closed && initialized) {
                    stall.sample(player, requestedPlaying && !ended, busy)
                    emit(VideoEvent.PROGRESS)
                    main.postDelayed(this, VIDEO_POLL_MS)
                }
            }
        }

    fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) {
        stop()
        if (closed) return
        callback = onEvent
        val token = generation
        requestedPlaying = true
        busy = true
        ended = false
        val next = PawPlayback(listener(token))
        next.controls.setHardwareEnabled(!software)
        next.controls.setPlaybackSpeed(speed)
        next.controls.setVolume(volume)
        next.controls.setMuted(muted)
        player = next
        emit(VideoEvent.OPENING)
        when {
            source.startsWith("file:") -> {
                initialize(File(URI(source)), next, token, null)
            }

            sourceKey == source && this.source != null -> {
                val existing = checkNotNull(this.source)
                existing.bind(next)
                existing.file?.let { initialize(it, next, token, existing) }
            }

            else -> {
                startSource(source, next)
            }
        }
    }

    private fun startSource(
        url: String,
        next: PawPlayback,
    ) {
        retireSource()
        sourceKey = url
        val identity = sourceSerial
        val download =
            ProgressiveSource(
                client,
                directory,
                url,
                object : ProgressiveSource.Listener {
                    override fun onFile(file: File) {
                        main.post {
                            if (closed || sourceSerial != identity) return@post
                            val active = player ?: return@post
                            val owned = source ?: return@post
                            initialize(file, active, generation, owned)
                        }
                    }

                    override fun onError(error: IOException) {
                        main.post {
                            if (!closed && sourceSerial == identity) {
                                sourceKey = null
                                emit(VideoEvent.ERROR)
                            }
                        }
                    }
                },
            )
        source = download
        download.bind(next)
        download.start()
    }

    private fun initialize(
        file: File,
        next: PawPlayback,
        token: Int,
        download: ProgressiveSource?,
    ) {
        if (initializingGeneration == token) return
        initializingGeneration = token
        worker.execute {
            if (closed || token != generation) return@execute
            try {
                PawPlayback.loadLibraries()
                next.init(file, download)
                main.post { finishOpen(next, token) }
            } catch (_: IOException) {
                main.post { if (!closed && token == generation) emit(VideoEvent.ERROR) }
            } catch (_: LinkageError) {
                main.post { if (!closed && token == generation) emit(VideoEvent.ERROR) }
            } catch (_: RuntimeException) {
                main.post { if (!closed && token == generation) emit(VideoEvent.ERROR) }
            }
        }
    }

    private fun finishOpen(
        next: PawPlayback,
        token: Int,
    ) {
        if (closed || token != generation) return
        initialized = true
        busy = false
        signalNextFrame()
        output.mount(next, initialized)
        next.controls.setVolume(volume)
        next.controls.setMuted(muted)
        next.controls.setPlaying(requestedPlaying)
        emit(if (requestedPlaying) VideoEvent.PLAYING else VideoEvent.PAUSED)
        main.post(polling)
    }

    fun signalNextFrame() {
        val token = generation
        player?.video?.onFrame {
            if (!closed && token == generation) emit(VideoEvent.VIDEO_OUTPUT)
        }
    }

    private fun listener(token: Int): PawPlayback.Listener =
        object : PawPlayback.Listener {
            override fun onError(player: PawPlayback) {
                if (!closed && token == generation) emit(VideoEvent.ERROR)
            }

            override fun onIncomplete(player: PawPlayback) {
                if (!closed && token == generation) {
                    ended = false
                    busy = false
                    requestedPlaying = false
                    player.controls.setPlaying(false)
                    Toast.makeText(noticeContext, R.string.video_incomplete, Toast.LENGTH_LONG).show()
                    emit(VideoEvent.INCOMPLETE)
                }
            }

            override fun onComplete(player: PawPlayback) {
                if (!closed && token == generation) {
                    ended = true
                    requestedPlaying = false
                    player.controls.setPlaying(false)
                    emit(VideoEvent.ENDED)
                }
            }

            override fun onBusyStateChange(
                player: PawPlayback,
                busy: Boolean,
            ) {
                if (!closed && token == generation) {
                    this@FfmpegVideoState.busy = busy
                    emit(VideoEvent.BUFFERING)
                }
            }

            override fun onDimensionChange(player: PawPlayback) {
                if (!closed && token == generation) emit(VideoEvent.PROGRESS)
            }

            override fun onVideoOutputReset(player: PawPlayback) {
                if (!closed && token == generation) {
                    signalNextFrame()
                    emit(VideoEvent.VIDEO_OUTPUT_PENDING)
                }
            }
        }

    fun emit(event: VideoEvent) {
        val active = player
        val duration = active?.facts?.duration?.coerceAtLeast(0L) ?: 0L
        VideoHandoffDiagnostics.engineEvent(this, event)
        callback(
            event,
            VideoPlaybackStatus(
                stage = if (busy || !initialized) VideoStage.BUFFERING else VideoStage.READY,
                position = active?.facts?.position?.coerceAtLeast(0L) ?: 0L,
                duration = duration,
                playing = initialized && requestedPlaying && !ended,
                seekable = initialized && duration > 0L,
                ended = ended,
            ),
        )
    }

    fun stop() {
        generation += 1
        main.removeCallbacks(polling)
        initialized = false
        stall.reset()
        source?.bind(null)
        player?.let { old ->
            old.setListener(null)
            old.controls.setPlaying(false)
            old.video.release()
            // Wake a native initialization/seek blocked waiting for bytes before
            // queueing destruction behind that operation.
            old.interruptRead()
            worker.execute { old.close() }
        }
        output.clear()
        player = null
    }

    private fun retireSource() {
        sourceSerial += 1
        source?.let { old ->
            old.close()
            worker.execute { old.deleteFile() }
        }
        source = null
        sourceKey = null
    }

    override fun close() {
        if (closed) return
        stop()
        closed = true
        retireSource()
        worker.shutdown()
        callback = { _, _ -> }
    }
}

private const val VIDEO_POLL_MS = 200L
