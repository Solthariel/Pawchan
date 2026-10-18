package com.solthariel.pawchan.media.video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** A replacement flow must start from its own value, not the previous collector's remembered value. */
@Composable
internal fun rememberVideoOutput(player: VideoPlaybackSession): State<Boolean> =
    key(player) { player.hasVideoOutput.collectAsStateWithLifecycle() }
