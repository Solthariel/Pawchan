package com.solthariel.pawchan.media.video

import android.os.SystemClock
import android.util.Log
import com.solthariel.pawchan.ffmpeg.PawPlayback

/** Rate-limited stall evidence without URLs, local paths or request credentials. */
internal class VideoStallDiagnostics {
    private var position = -1L
    private var since = 0L

    fun sample(
        player: PawPlayback?,
        playing: Boolean,
        busy: Boolean,
    ) {
        val now = SystemClock.elapsedRealtime()
        val current = player?.facts?.position ?: -1L
        if (!playing || current != position) {
            position = current
            since = now
        } else if (now - since >= STALL_REPORT_MS) {
            Log.w("PawchanVideoStall", "position=$current busy=$busy ${player?.controls?.diagnostics()}")
            since = now
        }
    }

    fun reset() {
        position = -1L
        since = 0L
    }
}

private const val STALL_REPORT_MS = 3_000L
