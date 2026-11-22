package com.solthariel.pawchan.media.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.media.LocalMediaRotation
import com.solthariel.pawchan.media.MediaViewerNavigation

@Composable
internal fun VideoControlRows(
    player: VideoPlaybackControls,
    progress: VideoPlaybackStatus,
    sound: VideoSoundState,
    navigation: MediaViewerNavigation,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scroll =
            if (maxWidth < CONTROL_BUTTONS_WIDTH) {
                Modifier.horizontalScroll(rememberScrollState())
            } else {
                Modifier
            }
        Row(
            Modifier.fillMaxWidth().then(scroll).height(CONTROLS_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SoundAndSpeed(player, sound)
            Spacer(Modifier.weight(1f))
            NavigationAndPlay(player, progress, navigation)
            Spacer(Modifier.weight(1f))
            VideoDecoderButton(progress, player::setSoftware)
            VideoOptionalButton(UiIconKind.ROTATE, stringResource(R.string.video_rotate), LocalMediaRotation.current)
        }
    }
}

@Composable
private fun SoundAndSpeed(
    player: VideoPlaybackControls,
    sound: VideoSoundState,
) {
    UiToolbarButton(
        if (!sound.hasAudio || sound.muted) UiIconKind.VOLUME_OFF else UiIconKind.VOLUME_ON,
        stringResource(if (sound.muted) R.string.video_unmute else R.string.video_mute),
        enabled = sound.hasAudio,
        onClick = player::toggleMuted,
    )
    VideoSpeedButton(sound.speed, player::cycleSpeed)
}

@Composable
private fun NavigationAndPlay(
    player: VideoPlaybackControls,
    progress: VideoPlaybackStatus,
    navigation: MediaViewerNavigation,
) {
    UiToolbarButton(
        UiIconKind.PREVIOUS,
        stringResource(R.string.video_previous),
        enabled = navigation.previous != null,
        onClick = { navigation.previous?.invoke() },
    )
    VideoPlayButton(progress.playing, player::togglePlay)
    UiToolbarButton(
        UiIconKind.NEXT,
        stringResource(R.string.video_next),
        enabled = navigation.next != null,
        onClick = { navigation.next?.invoke() },
    )
}

@Composable
private fun VideoOptionalButton(
    kind: UiIconKind,
    description: String,
    onClick: (() -> Unit)?,
) {
    if (onClick != null) {
        UiToolbarButton(kind, description, onClick = onClick)
    } else {
        Spacer(Modifier.size(APP_TOOLBAR_BUTTON_SIZE))
    }
}

@Composable
private fun VideoPlayButton(
    playing: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(if (playing) R.string.video_pause else R.string.video_play)
    Box(
        Modifier
            .size(PLAY_BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        UiIcon(
            if (playing) UiIconKind.PAUSE else UiIconKind.PLAY,
            MaterialTheme.colorScheme.onSurface,
            Modifier.size(PLAY_ICON_SIZE),
        )
    }
}

@Composable
private fun VideoSpeedButton(
    speed: VideoSpeed,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.video_speed, speed.label)
    Box(
        Modifier
            .size(APP_TOOLBAR_BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            speed.label,
            color =
                if (speed == VideoSpeed.NORMAL) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
        )
    }
}

@Composable
private fun VideoDecoderButton(
    progress: VideoPlaybackStatus,
    onSwitch: (Boolean) -> Unit,
) {
    val hardware = progress.hardwareDecoded
    val description =
        when (hardware) {
            true -> stringResource(R.string.video_decoder_hw)
            false -> stringResource(R.string.video_decoder_sw)
            null -> stringResource(R.string.video_decoder_pending)
        }
    Box(
        Modifier
            .size(APP_TOOLBAR_BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(enabled = hardware != null) { if (hardware != null) onSwitch(hardware) }
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (hardware == null) {
                "…"
            } else if (hardware) {
                "HW"
            } else {
                "SW"
            },
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

private const val CONTROL_COUNT = 7
private val CONTROL_BUTTONS_WIDTH = APP_TOOLBAR_BUTTON_SIZE * CONTROL_COUNT
private val CONTROLS_HEIGHT = 48.dp
private val PLAY_BUTTON_SIZE = 48.dp
private val PLAY_ICON_SIZE = 32.dp
