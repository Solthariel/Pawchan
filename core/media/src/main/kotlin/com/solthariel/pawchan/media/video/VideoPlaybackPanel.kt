package com.solthariel.pawchan.media.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.media.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.media.LocalMediaRotation
import com.solthariel.pawchan.media.MediaViewerNavigation

/**
 * Нижняя панель видео: тонкая строка перемотки и компактная строка кнопок.
 * Слева звук и скорость, по центру предыдущий/play/следующий, справа PiP и поворот —
 * по два слота с каждой стороны, поэтому play ровно по центру экрана.
 */
@Composable
internal fun VideoPlaybackPanel(
    player: VideoPlaybackControls,
    navigation: MediaViewerNavigation = MediaViewerNavigation(),
    onPip: (() -> Unit)? = null,
) {
    val progress by player.status.collectAsStateWithLifecycle()
    val sound by player.sound.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxWidth().padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET)) {
        VideoSeekRow(player, progress)
        Row(
            Modifier.fillMaxWidth().height(VIDEO_CONTROLS_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UiToolbarButton(
                if (!sound.hasAudio || sound.muted) UiIconKind.VOLUME_OFF else UiIconKind.VOLUME_ON,
                stringResource(if (sound.muted) R.string.video_unmute else R.string.video_mute),
                enabled = sound.hasAudio,
                onClick = player::toggleMuted,
            )
            VideoSpeedButton(sound.speed, player::cycleSpeed)
            Spacer(Modifier.weight(1f))
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
            Spacer(Modifier.weight(1f))
            VideoOptionalButton(UiIconKind.PIP, stringResource(R.string.video_pip), onPip)
            VideoOptionalButton(UiIconKind.ROTATE, stringResource(R.string.video_rotate), LocalMediaRotation.current)
        }
    }
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
            .padding(horizontal = 4.dp)
            .size(VIDEO_PLAY_BUTTON_SIZE)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        UiIcon(
            if (playing) UiIconKind.PAUSE else UiIconKind.PLAY,
            MaterialTheme.colorScheme.onSurface,
            Modifier.size(VIDEO_PLAY_ICON_SIZE),
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
private fun VideoSeekRow(
    player: VideoPlaybackControls,
    progress: VideoPlaybackStatus,
) {
    var scrubbing by remember(player) { mutableStateOf<Float?>(null) }
    val position = scrubbing?.toLong() ?: progress.position
    Row(Modifier.fillMaxWidth().height(VIDEO_SEEK_ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        VideoTimeLabel(videoTimeLabel(position))
        // Строка 28dp: Slider сам расширился бы до 48dp минимальной области касания.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            VideoSeekSlider(
                position.toFloat(),
                progress,
                onChange = { scrubbing = it },
                onFinish = {
                    scrubbing?.let { player.seekTo(it.toLong()) }
                    scrubbing = null
                },
            )
        }
        VideoTimeLabel(videoDurationLabel(progress.duration))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.VideoSeekSlider(
    position: Float,
    progress: VideoPlaybackStatus,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit,
) {
    val seekable = progress.duration > 0L && progress.seekable
    Slider(
        value = if (seekable) position else 0f,
        onValueChange = onChange,
        onValueChangeFinished = onFinish,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        enabled = seekable,
        valueRange = 0f..progress.duration.coerceAtLeast(1L).toFloat(),
        thumb = { VideoSeekThumb(seekable) },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state,
                modifier = Modifier.height(VIDEO_TRACK_HEIGHT),
                enabled = seekable,
                drawStopIndicator = null,
                thumbTrackGapSize = 0.dp,
                trackInsideCornerSize = 0.dp,
            )
        },
    )
}

@Composable
private fun VideoSeekThumb(enabled: Boolean) {
    // Видимая точка 12dp; область касания даёт сам Slider (по высоте 48dp).
    Box(Modifier.size(VIDEO_THUMB_TOUCH), contentAlignment = Alignment.Center) {
        val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        Box(Modifier.size(VIDEO_THUMB_SIZE).background(color, CircleShape))
    }
}

@Composable
private fun VideoTimeLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        // Цифры одной ширины: подпись не «дёргается» во время воспроизведения.
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
    )
}

private val VIDEO_TRACK_HEIGHT = 3.dp
private val VIDEO_SEEK_ROW_HEIGHT = 28.dp
private val VIDEO_CONTROLS_HEIGHT = 44.dp
private val VIDEO_PLAY_BUTTON_SIZE = 44.dp
private val VIDEO_PLAY_ICON_SIZE = 32.dp
private val VIDEO_THUMB_SIZE = 12.dp
private val VIDEO_THUMB_TOUCH = 20.dp
