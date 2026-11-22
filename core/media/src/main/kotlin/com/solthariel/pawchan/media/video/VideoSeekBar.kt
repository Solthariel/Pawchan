package com.solthariel.pawchan.media.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoSeekRow(
    player: VideoPlaybackControls,
    progress: VideoPlaybackStatus,
    seekColors: ColorScheme = MaterialTheme.colorScheme,
) {
    var scrubbing by remember(player) { mutableStateOf<Float?>(null) }
    val position = scrubbing?.toLong() ?: progress.position
    Row(Modifier.fillMaxWidth().height(SEEK_ROW_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        VideoTimeLabel(videoTimeLabel(position))
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            VideoSeekSlider(
                position.toFloat(),
                progress,
                seekColors,
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
    seekColors: ColorScheme,
    onChange: (Float) -> Unit,
    onFinish: () -> Unit,
) {
    val seekable = progress.duration > 0L && progress.seekable
    Slider(
        value = if (seekable) position else 0f,
        onValueChange = onChange,
        onValueChangeFinished = onFinish,
        modifier = Modifier.weight(1f).padding(horizontal = 12.dp).semantics { contentDescription = "Перемотка видео" },
        enabled = seekable,
        valueRange = 0f..progress.duration.coerceAtLeast(1L).toFloat(),
        thumb = { VideoSeekThumb(seekable, seekColors) },
        track = { state ->
            VideoSeekTrack(
                fraction = (state.value / state.valueRange.endInclusive).coerceIn(0f, 1f),
                enabled = seekable,
                colors = seekColors,
            )
        },
    )
}

@Composable
private fun VideoSeekThumb(
    enabled: Boolean,
    colors: ColorScheme,
) {
    Box(Modifier.size(THUMB_TOUCH), contentAlignment = Alignment.Center) {
        val color = videoSeekActiveColor(colors, enabled)
        Box(Modifier.size(THUMB_SIZE).background(color, CircleShape))
    }
}

@Composable
private fun VideoTimeLabel(text: String) {
    Box(Modifier.widthIn(min = APP_TOOLBAR_BUTTON_SIZE), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            maxLines = 1,
        )
    }
}

private val SEEK_ROW_HEIGHT = 48.dp
private val THUMB_SIZE = 12.dp
private val THUMB_TOUCH = 48.dp
