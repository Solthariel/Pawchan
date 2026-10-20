package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.video.VideoPlaybackControls
import com.solthariel.pawchan.media.video.VideoPlaybackPanel
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoSoundState
import com.solthariel.pawchan.media.video.VideoSpeed
import com.solthariel.pawchan.media.video.VideoStage
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoPlaybackPanelRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unknownDurationStillShowsTimeLabelsAndPlaybackControls() {
        compose.setContent {
            val player = remember { TestVideoControls() }
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize()) { VideoPlaybackPanel(player) }
            }
        }
        compose.onAllNodesWithText("0:00").assertCountEquals(2)
        compose.onNodeWithContentDescription("Воспроизвести").assertIsDisplayed()
        compose.onNodeWithContentDescription("Воспроизвести").performClick()
        compose.onNodeWithContentDescription("Пауза").assertIsDisplayed()
    }

    @Test
    fun errorKeepsControlsAndNeighborNavigationAvailable() {
        val player = TestVideoControls()
        var next = 0
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize()) {
                    VideoPlaybackPanel(player, MediaViewerNavigation(next = { next += 1 }))
                }
            }
        }
        compose.runOnIdle { player.status.value = player.status.value.copy(stage = VideoStage.ERROR) }
        compose.onAllNodesWithText("0:00").assertCountEquals(2)
        compose.onNodeWithContentDescription("Воспроизвести").assertIsDisplayed()
        compose.onNodeWithContentDescription("Следующий файл").performClick()
        compose.runOnIdle { assertEquals(1, next) }
    }
}

private class TestVideoControls : VideoPlaybackControls {
    override val status = MutableStateFlow(VideoPlaybackStatus(stage = VideoStage.READY))

    override fun togglePlay() {
        status.value = status.value.copy(playing = !status.value.playing)
    }

    override fun seekTo(position: Long) {
        status.value = status.value.copy(position = position)
    }

    override fun setSoftware(enabled: Boolean) {
        status.value = status.value.copy(attempt = status.value.attempt.copy(software = enabled))
    }

    override val sound = MutableStateFlow(VideoSoundState())

    override fun toggleMuted() {
        sound.value = sound.value.copy(muted = !sound.value.muted)
    }

    override fun cycleSpeed() = setSpeed(sound.value.speed.next())

    override fun setSpeed(speed: VideoSpeed) {
        sound.value = sound.value.copy(speed = speed)
    }

    override fun retry() = Unit
}
