package com.solthariel.pawchan.media.video

import android.content.Context
import android.os.Handler
import android.util.Log
import com.solthariel.pawchan.ffmpeg.ProgressiveSource
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.net.URI

/** libmpv backend; all libmpv calls are serialized on its owned worker. */
internal class MpvVideoEngine(
    context: Context,
    private val client: OkHttpClient?,
    private val directory: File,
    frameCounters: ((MpvFrameCounters) -> Unit)? = null,
) : MpvSurfaceBackend {
    private val main = Handler(context.mainLooper)
    private val worker =
        MpvWorker { generation, operation, error ->
            Log.e("PawMpv", "$operation failure: ${error.javaClass.simpleName}")
            main.post {
                if (generation == token && !closed) {
                    if (error is IOException || sourceFailed) {
                        events.sourceError()
                    } else {
                        events.emit(VideoEvent.ERROR)
                    }
                }
            }
        }

    @Volatile private var token = 0

    @Volatile private var closed = false

    private var source: ProgressiveSource? = null // main thread
    private val sourceFailed: Boolean get() = source?.failed == true || nativeSession.inputFailed

    private val events = MpvPlaybackEvents()

    @Volatile private var facts: Map<String, String> = emptyMap()

    @Volatile private var wantsPlay = true

    @Volatile private var volumeValue = MAX_VOLUME_PERCENT

    @Volatile private var mutedValue = false

    @Volatile private var speedValue = NORMAL_SPEED_MILLI

    private val surface: MpvSurfaceController =
        MpvSurfaceController(
            object : MpvSurfaceHost {
                override val token: Int get() = this@MpvVideoEngine.token
                override val closed: Boolean get() = this@MpvVideoEngine.closed
                override val handle: Long get() = nativeSession.handle

                override fun onMain(action: () -> Unit) {
                    main.post(action)
                }

                override fun onWorker(
                    token: Int,
                    action: () -> Unit,
                ) {
                    worker.execute(token, "surface", action)
                }

                override fun onEvent(event: VideoEvent) = events.emit(event)

                override fun setProperty(
                    name: String,
                    value: String,
                ) = nativeSession.setProperty(name, value)
            },
        )

    private val poller: MpvPoller =
        MpvPoller(
            object : MpvPollHost {
                override val token: Int get() = this@MpvVideoEngine.token
                override val closed: Boolean get() = this@MpvVideoEngine.closed
                override val handle: Long get() = nativeSession.handle
                override var facts: Map<String, String>
                    get() = this@MpvVideoEngine.facts
                    set(value) {
                        this@MpvVideoEngine.facts = value
                    }

                override fun scheduleWorker(
                    token: Int,
                    action: () -> Unit,
                ) {
                    worker.schedule(token, "poll", POLL_INTERVAL_MILLIS, action)
                }

                override fun onMain(action: () -> Unit) {
                    main.post(action)
                }

                override fun onSample(sample: MpvPollSample) = events.sample(sample, nativeSession.waitingForBytes)

                override fun onEnd(reason: String?) {
                    if (sourceFailed) events.sourceError() else events.end(reason)
                }

                override fun onPlaybackRestart() = surface.playbackRestarted()
            },
            frameCounters,
        )

    private val nativeSession: MpvNativeSession =
        MpvNativeSession(
            context.applicationContext,
            worker,
            object : MpvNativeSessionHost {
                override val token: Int get() = this@MpvVideoEngine.token
                override val closed: Boolean get() = this@MpvVideoEngine.closed
                override val wantsPlay: Boolean get() = this@MpvVideoEngine.wantsPlay
                override val volume: Int get() = volumeValue
                override val muted: Boolean get() = mutedValue
                override val speed: Int get() = speedValue

                override fun mountSurface(handle: Long) = surface.mountCurrent(handle)

                override fun onEngineDestroyed() = surface.engineDestroyed()

                override fun onStarted(token: Int) = poller.schedule(token)
            },
        )

    override fun open(
        source: String,
        software: Boolean,
        onEvent: (VideoEvent, VideoPlaybackStatus) -> Unit,
    ) {
        stop()
        if (closed) return
        events.open(onEvent)
        val current = token
        wantsPlay = true
        Log.i("PawMpv", "open mode=${if (software) "software" else "auto"}")
        events.emit(VideoEvent.OPENING)
        if (source.startsWith("file:")) {
            nativeSession.start(File(URI.create(source)), null, software, current)
        } else {
            val download =
                ProgressiveSource(
                    requireNotNull(client) { "Network video requires a client" },
                    directory,
                    source,
                    object : ProgressiveSource.Listener {
                        override fun onFile(file: File) {
                            main.post {
                                if (current == token &&
                                    !closed
                                ) {
                                    nativeSession.start(file, this@MpvVideoEngine.source, software, current)
                                }
                            }
                        }

                        override fun onError(error: IOException) {
                            Log.w("PawMpv", "source failure: ${error.javaClass.simpleName}")
                            main.post { if (current == token && !closed) events.sourceError() }
                        }
                    },
                )
            this.source = download
            download.start()
        }
    }

    override fun play() {
        wantsPlay = true
        events.resumeOrSeek()
        nativeSession.setProperty("pause", "no")
    }

    override fun replay() {
        if (closed) return
        wantsPlay = true
        events.resumeOrSeek()
        nativeSession.replay(token)
    }

    override fun pause() {
        wantsPlay = false
        nativeSession.setProperty("pause", "yes")
    }

    override fun seekTo(position: Long) {
        events.resumeOrSeek()
        nativeSession.setProperty("time-pos", (position.coerceAtLeast(0) / MILLIS_PER_SECOND).toString())
    }

    override fun volume(percent: Int) {
        volumeValue = percent.coerceIn(0, MAX_VOLUME_PERCENT)
        nativeSession.setProperty("volume", volumeValue.toString())
    }

    override fun muted(value: Boolean) {
        mutedValue = value
        nativeSession.setProperty("mute", if (value) "yes" else "no")
    }

    override fun speed(milli: Int): Int {
        speedValue = milli
        nativeSession.setProperty("speed", (milli / MILLIS_PER_SECOND).toString())
        return milli
    }

    override val metadata: Map<String, String> get() = facts
    override val renderOutput: MpvVideoOutput get() = surface.output

    override fun stop() {
        token++
        surface.prepareStop()
        val oldSource = source
        source = null
        nativeSession.stop(oldSource)
        facts = emptyMap()
    }

    override fun close() {
        if (closed) return
        surface.output.close()
        stop()
        closed = true
        events.close()
        worker.shutdown()
    }
}

private const val MAX_VOLUME_PERCENT = 100
private const val NORMAL_SPEED_MILLI = 1000
private const val MILLIS_PER_SECOND = 1000.0
private const val POLL_INTERVAL_MILLIS = 200L
