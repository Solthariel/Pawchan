package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.video.VideoPlaybackControls
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoSeekRow
import com.solthariel.pawchan.media.video.VideoSoundState
import com.solthariel.pawchan.media.video.VideoSpeed
import com.solthariel.pawchan.media.video.VideoTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoSeekTargetRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun seekThumbHasAtLeast48DpTouchTarget() {
        val controls = SeekControls()
        compose.setContent {
            MaterialTheme {
                VideoSeekRow(
                    controls,
                    VideoPlaybackStatus(
                        seekable = true,
                        timeline = VideoTimeline(position = 1_000L, duration = 10_000L),
                    ),
                )
            }
        }
        val bounds = compose.onNodeWithContentDescription("Перемотка видео").fetchSemanticsNode().boundsInRoot
        val required = with(compose.density) { 48.dp.toPx() }
        assertTrue("Seek control is shorter than 48 dp", bounds.height >= required)
    }
}

private class SeekControls : VideoPlaybackControls {
    override val status = MutableStateFlow(VideoPlaybackStatus())
    override val sound = MutableStateFlow(VideoSoundState())

    override fun togglePlay() = Unit

    override fun seekTo(position: Long) = Unit

    override fun setSoftware(enabled: Boolean) = Unit

    override fun retry() = Unit

    override fun toggleMuted() = Unit

    override fun cycleSpeed() = Unit

    override fun setSpeed(speed: VideoSpeed) = Unit
}
