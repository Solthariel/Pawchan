package com.solthariel.pawchan.media.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.media.R
import kotlinx.coroutines.delay

@Composable
internal fun VideoStateOverlay(
    state: VideoPlaybackStatus,
    waitingForFrame: Boolean = false,
    onRetry: () -> Unit,
) {
    if (state.stage == VideoStage.ERROR) {
        val message =
            when (state.attempt.failure) {
                VideoFailure.SOURCE -> R.string.video_source_failed
                else -> R.string.video_load_failed
            }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(message), color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.retry)) }
        }
    } else if (!state.ended && (state.stage == VideoStage.BUFFERING || waitingForFrame)) {
        DelayedVideoLoading()
    }
}

@Composable
private fun DelayedVideoLoading() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(VIDEO_BUFFER_HINT_MS)
        visible = true
    }
    if (visible) {
        Box(Modifier.fillMaxSize().padding(top = 64.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                stringResource(R.string.video_buffer_hint),
                modifier =
                    Modifier
                        .background(Color.Black.copy(alpha = HINT_OPACITY), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

internal const val VIDEO_BUFFER_HINT_MS = 650L
private const val HINT_OPACITY = 0.72f
