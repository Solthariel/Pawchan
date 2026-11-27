package com.solthariel.pawchan.media.video

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.solthariel.pawchan.media.MediaAttachmentUi

/** Temporary, bounded handoff tracing. No source URLs, filenames, cookies or post text. */
internal object VideoHandoffDiagnostics {
    fun record(
        owner: Any,
        message: String,
    ) {
        Log.d(TAG, "t=${SystemClock.elapsedRealtime()} owner=${System.identityHashCode(owner)} $message")
    }
}

@Composable
internal fun BindVideoSessionDiagnostics(
    video: MediaAttachmentUi,
    player: VideoPlaybackSession,
    origin: String,
) {
    DisposableEffect(player, video.id) {
        VideoHandoffDiagnostics.record(
            player,
            "viewer_bind item=${video.id.hashCode()} engine=${System.identityHashCode(player.engine)} " +
                "origin=$origin output=${player.hasVideoOutput.value}",
        )
        onDispose { VideoHandoffDiagnostics.record(player, "viewer_dispose") }
    }
}

private const val TAG = "PawchanVideoHandoff"
