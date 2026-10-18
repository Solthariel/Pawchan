package com.solthariel.pawchan.media.video

import android.graphics.Bitmap
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** One bounded rendered frame per open viewer, never a process-wide cache or a disk thumbnail. */
internal class VideoPosterStore {
    var frame: Bitmap? by mutableStateOf(null)
        private set

    fun capture(player: VideoPlaybackSession) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (player.hasVideoOutput.value && !player.inPip) {
            val next = player.engine.posterFrame
            if (next != null) frame = next
            VideoHandoffDiagnostics.record(player, "poster_capture available=${next != null}")
        }
    }

    fun clear() {
        // Do not recycle: an outgoing Compose draw may still hold the previous bitmap.
        frame = null
    }
}

internal val LocalVideoPosterStore = staticCompositionLocalOf<VideoPosterStore?> { null }

@Composable
internal fun rememberVideoPosterStore(): VideoPosterStore {
    val store = remember { VideoPosterStore() }
    DisposableEffect(store) { onDispose { store.clear() } }
    return store
}

internal const val VIDEO_POSTER_MAX_EDGE = 512

/** Immutable reserve for one incoming handoff, independent of later shared-store captures. */
@Composable
internal fun rememberVideoPosterReserve(owner: Any): Bitmap? {
    val store = LocalVideoPosterStore.current
    return remember(owner, store) { store?.frame }
}
