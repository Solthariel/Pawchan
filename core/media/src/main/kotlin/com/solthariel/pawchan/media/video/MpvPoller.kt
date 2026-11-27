package com.solthariel.pawchan.media.video

import android.os.SystemClock
import android.util.Log
import io.github.yuroyami.libmpvkt.NodeCodec
import io.github.yuroyami.libmpvkt.asMap
import io.github.yuroyami.libmpvkt.asString
import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi

/** Engine-owned access to the worker, callbacks and current playback generation. */
internal interface MpvPollHost {
    val token: Int
    val closed: Boolean
    val handle: Long
    var facts: Map<String, String>

    fun scheduleWorker(
        token: Int,
        action: () -> Unit,
    )

    fun onMain(action: () -> Unit)

    fun onSample(sample: MpvPollSample)

    fun onEnd(reason: String?)

    fun onPlaybackRestart()
}

/** All native reads occur on the engine worker; UI updates are posted to the main thread. */
@OptIn(MpvNativeApi::class)
internal class MpvPoller(
    private val host: MpvPollHost,
    private val onFrameCounters: ((MpvFrameCounters) -> Unit)? = null,
) {
    private var lastLogAt = 0L
    private var lastDetailsAt = 0L
    private var lastToken = -1
    private var previous: MpvPollSample? = null

    fun schedule(current: Int) {
        host.scheduleWorker(current) {
            if (current != host.token || host.closed || host.handle == 0L) return@scheduleWorker
            drainEvents(current)
            val handle = host.handle
            if (lastToken != current) {
                lastToken = current
                previous = null
                lastDetailsAt = 0L
            }
            val now = SystemClock.elapsedRealtime()
            val refresh = shouldRefreshMpvDetails(previous?.hasFile == true, now - lastDetailsAt)
            val sample = readMpvPoll(handle, previous.takeUnless { refresh })
            if (refresh && sample.hasFile) lastDetailsAt = now
            previous = sample
            if (refresh || !sample.hasFile) host.facts = sample.facts
            val measured = if (sample.hasFile && onFrameCounters != null) readMpvFrameCounters(handle, sample) else null
            if (measured != null) onFrameCounters?.invoke(measured)
            logSample(handle, sample, measured)
            host.onMain {
                if (current == host.token && !host.closed) host.onSample(sample)
            }
            schedule(current)
        }
    }

    private fun drainEvents(current: Int) {
        repeat(MAX_EVENTS_PER_POLL) {
            val event = NodeCodec.decodeEvent(MpvNative.waitEvent(host.handle, 0.0))
            if (event.eventId == 0) return
            if (event.eventId == MPV_EVENT_PLAYBACK_RESTART) {
                host.onMain {
                    if (current == host.token && !host.closed) host.onPlaybackRestart()
                }
            }
            if (event.eventId == MPV_EVENT_END_FILE) {
                val reason =
                    event.node
                        .asMap()
                        ?.get("reason")
                        ?.asString()
                Log.i("PawMpv", "end reason=$reason")
                host.onMain {
                    if (current == host.token && !host.closed) host.onEnd(reason)
                }
            }
        }
    }

    private fun logSample(
        handle: Long,
        sample: MpvPollSample,
        measured: MpvFrameCounters?,
    ) {
        val now = SystemClock.elapsedRealtime()
        if (sample.hasFile && now - lastLogAt >= LOG_INTERVAL_MILLIS) {
            lastLogAt = now
            val facts = host.facts
            val counters = measured ?: readMpvFrameCounters(handle, sample)
            Log.i(
                "PawMpv",
                "decoder=${sample.decoder} size=${facts["width"]}x${facts["height"]} " +
                    "fps=${facts["frame_rate"]} decoder_drops=${counters.decoderDrops} " +
                    "vo_drops=${counters.outputDrops}",
            )
        }
    }
}

/** Refresh codec and metadata on file open, after unload, and every few seconds. */
internal fun shouldRefreshMpvDetails(
    previousHadFile: Boolean,
    elapsedMillis: Long,
): Boolean = !previousHadFile || elapsedMillis >= DETAILS_REFRESH_MILLIS

private const val DETAILS_REFRESH_MILLIS = 2_000L
private const val LOG_INTERVAL_MILLIS = 5000L
private const val MAX_EVENTS_PER_POLL = 128
private const val MPV_EVENT_END_FILE = 7
private const val MPV_EVENT_PLAYBACK_RESTART = 21
