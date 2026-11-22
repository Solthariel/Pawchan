package com.solthariel.pawchan.media.video

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.media.MediaViewerNavigation

/** A compact seek row and a single row of playback controls. */
@Composable
internal fun VideoPlaybackPanel(
    player: VideoPlaybackControls,
    navigation: MediaViewerNavigation = MediaViewerNavigation(),
    seekColors: ColorScheme = MaterialTheme.colorScheme,
) {
    val progress by player.status.collectAsStateWithLifecycle()
    val sound by player.sound.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET)) {
        VideoSeekRow(player, progress, seekColors)
        VideoControlRows(player, progress, sound, navigation)
    }
}
