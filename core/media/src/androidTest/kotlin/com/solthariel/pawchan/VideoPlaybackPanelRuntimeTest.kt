package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.video.VideoPlaybackControls
import com.solthariel.pawchan.media.video.VideoPlaybackPanel
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoSoundState
import com.solthariel.pawchan.media.video.VideoSpeed
import com.solthariel.pawchan.media.video.VideoStage
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoPlaybackPanelRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun narrowPanelKeepsTransportCenteredAndEveryButtonInsideViewport() {
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.width(320.dp).testTag("video-panel")) {
                    VideoPlaybackPanel(TestVideoControls(), onPip = {})
                }
            }
        }
        val viewport = compose.onNodeWithTag("video-panel").fetchSemanticsNode().boundsInRoot
        val labels = listOf("Предыдущий файл", "Воспроизвести", "Следующий файл", "Выключить звук")
        val minimum = with(compose.density) { 48.dp.toPx() }
        labels.forEach {
            val bounds = compose.onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot
            assertTrue(bounds.left >= viewport.left && bounds.right <= viewport.right)
            assertTrue(bounds.width >= minimum - 1f && bounds.height >= minimum - 1f)
        }
        val play = compose.onNodeWithContentDescription("Воспроизвести").fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.center.x, play.center.x, 1f)
    }

    @Test
    fun unknownDurationStillShowsTimeLabelsAndPlaybackControls() {
        compose.setContent {
            val player = remember { TestVideoControls() }
            MaterialTheme(colorScheme = darkColorScheme()) {
                Box(Modifier.fillMaxSize()) { VideoPlaybackPanel(player) }
            }
        }
        compose.onAllNodesWithText("0:00").assertCountEquals(1)
        compose.onAllNodesWithText("—:—").assertCountEquals(1)
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
        compose.onAllNodesWithText("0:00").assertCountEquals(1)
        compose.onAllNodesWithText("—:—").assertCountEquals(1)
        compose.onNodeWithContentDescription("Воспроизвести").assertIsDisplayed()
        compose.onNodeWithContentDescription("Следующий файл").performClick()
        compose.runOnIdle { assertEquals(1, next) }
    }

    @Test
    fun muteIsDisabledForSilentVideoAndEnabledWhenAudioIsPresent() {
        val player = TestVideoControls()
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { VideoPlaybackPanel(player) }
        }
        compose.onNodeWithContentDescription("Выключить звук").assertIsNotEnabled()
        compose.runOnIdle { player.sound.value = player.sound.value.copy(hasAudio = true) }
        compose.onNodeWithContentDescription("Выключить звук").assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Включить звук").assertIsEnabled()
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
