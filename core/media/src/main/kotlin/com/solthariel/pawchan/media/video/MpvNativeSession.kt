package com.solthariel.pawchan.media.video

import android.content.Context
import com.solthariel.pawchan.ffmpeg.MpvProgressiveProvider
import com.solthariel.pawchan.ffmpeg.ProgressiveSource
import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi
import java.io.File

/** Values owned by the engine and callbacks back into its worker/main-thread coordinators. */
internal interface MpvNativeSessionHost {
    val token: Int
    val closed: Boolean
    val wantsPlay: Boolean
    val volume: Int
    val muted: Boolean
    val speed: Int

    fun mountSurface(handle: Long)

    fun onEngineDestroyed()

    fun onStarted(token: Int)
}

/** Owns a native core and its provider. All handle operations run on the same worker. */
@OptIn(MpvNativeApi::class)
internal class MpvNativeSession(
    private val app: Context,
    private val worker: MpvWorker,
    private val host: MpvNativeSessionHost,
) {
    var handle = 0L
        private set // worker thread only

    @Volatile private var provider: MpvProgressiveProvider? = null

    fun start(
        file: File,
        download: ProgressiveSource?,
        software: Boolean,
        current: Int,
    ) {
        worker.execute(current, "open") {
            if (current != host.token || host.closed) return@execute
            MpvNative.initAndroid(app)
            val h = MpvNative.create()
            check(h != 0L)
            handle = h
            configureMpvOptions(h, software)
            check(MpvNative.initialize(h) >= 0)
            val next = MpvProgressiveProvider(file, download, Runnable {}, Runnable {})
            provider = next
            if (current != host.token || host.closed) {
                next.close()
                return@execute
            }
            check(MpvNative.streamCbAddRo(h, "pawvideo", next) >= 0)
            host.mountSurface(h)
            check(MpvNative.command(h, arrayOf("loadfile", "pawvideo://video")) >= 0)
            MpvNative.setPropertyString(h, "volume", host.volume.toString())
            MpvNative.setPropertyString(h, "mute", if (host.muted) "yes" else "no")
            MpvNative.setPropertyString(h, "speed", (host.speed / MILLIS_PER_SECOND).toString())
            MpvNative.setPropertyString(h, "pause", if (host.wantsPlay) "no" else "yes")
            host.onStarted(current)
        }
    }

    fun setProperty(
        name: String,
        value: String,
    ) {
        worker.execute(host.token, "property") {
            if (handle != 0L) MpvNative.setPropertyString(handle, name, value)
        }
    }

    fun stop(source: ProgressiveSource?) {
        provider?.close()
        provider = null
        source?.close()
        worker.execute(host.token, "stop") {
            if (handle != 0L) {
                MpvNative.terminateDestroy(handle)
                handle = 0L
            }
            host.onEngineDestroyed()
            // Native stream has been destroyed. HTTP writers remove their temp file
            // asynchronously once both workers terminate; never block mpv's queue.
            source?.deleteFile()
        }
    }
}

private const val MILLIS_PER_SECOND = 1000.0
