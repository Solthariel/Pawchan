package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.solthariel.pawchan.domain.settings.VideoCompletion
import com.solthariel.pawchan.media.LocalMediaPreferences

@Composable
internal fun BindVideoCompletion(
    player: VideoPlaybackSession,
    status: VideoPlaybackStatus,
) {
    val completion = LocalMediaPreferences.current.completion
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(player, status.ended, completion) {
        val visible = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !player.inPip
        if (status.ended && completion == VideoCompletion.REPEAT && visible) {
            player.startAt(0L)
        }
    }
}
