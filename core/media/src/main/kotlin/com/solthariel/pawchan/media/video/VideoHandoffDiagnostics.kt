package com.solthariel.pawchan.media.video

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.MediaAttachmentUi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

/** Temporary, bounded handoff tracing. No source URLs, filenames, cookies or post text. */
internal object VideoHandoffDiagnostics {
    fun record(
        owner: Any,
        message: String,
    ) {
        Log.d(TAG, "t=${SystemClock.elapsedRealtime()} owner=${System.identityHashCode(owner)} $message")
    }

    fun engineEvent(
        state: FfmpegVideoState,
        event: VideoEvent,
    ) {
        if (event in OUTPUT_EVENTS) {
            record(state, "engine_event=$event native=${System.identityHashCode(state.player)}")
        }
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

@Composable
internal fun BindVideoPosterDiagnostics(
    player: VideoPlaybackSession,
    hasVideoOutput: Boolean,
    alpha: State<Float>,
    motion: ImageViewerMotion,
) {
    DisposableEffect(player, hasVideoOutput) {
        VideoHandoffDiagnostics.record(player, "poster_output_commit output=$hasVideoOutput alpha=${alpha.value}")
        onDispose { }
    }
    LaunchedEffect(player, hasVideoOutput, alpha, motion) {
        snapshotFlow {
            "poster output=$hasVideoOutput mounted=${!hasVideoOutput || alpha.value > 0f} " +
                "alpha_step=${alphaStep(alpha.value)} image_step=${alphaStep(motion.imageAlpha)} " +
                "window_step=${alphaStep(motion.windowAlpha)}"
        }.distinctUntilChanged().collect { VideoHandoffDiagnostics.record(player, it) }
    }
}

@Composable
internal fun rememberPosterTrace(player: VideoPlaybackSession): PosterTrace = remember(player) { PosterTrace(player) }

internal class PosterTrace(
    private val player: VideoPlaybackSession,
) {
    private var previous: String? = null
    private var previousDrawStep: Int? = null

    fun record(state: String) {
        if (state != previous) {
            previous = state
            VideoHandoffDiagnostics.record(player, "poster_bitmap=$state")
        }
    }

    fun draw(alpha: Float) {
        val step = alphaStep(alpha)
        if (step != previousDrawStep) {
            previousDrawStep = step
            VideoHandoffDiagnostics.record(player, "poster_draw alpha_step=$step")
        }
    }
}

private fun alphaStep(value: Float): Int = (value * ALPHA_STEPS).roundToInt()

private const val TAG = "PawchanVideoHandoff"
private const val ALPHA_STEPS = 10
private val OUTPUT_EVENTS =
    setOf(VideoEvent.OPENING, VideoEvent.VIDEO_OUTPUT_PENDING, VideoEvent.VIDEO_OUTPUT, VideoEvent.ERROR)
