package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.solthariel.pawchan.media.LocalMediaPickerPlayback

@Composable
internal fun BindVideoPickerReturn(
    session: VideoPlaybackSession,
    access: VideoPrivacyAccess?,
) {
    val picker = LocalMediaPickerPlayback.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(picker, session, lifecycle, access) {
        picker?.bind(
            isPlaying = { session.wantsPlayback && !session.inPip },
            onResume = {
                if (access?.isAllowed() != false && !session.inPip) session.resumeAfterPicker()
            },
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED),
        )
        onDispose { picker?.clear() }
    }
}

internal fun VideoPlaybackSession.resumeAfterPicker() {
    val current = status.value
    if (current.stage == VideoStage.ERROR || current.ended) return
    if (!current.playing && !wantsPlayback) togglePlay()
}
