package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.LocalMediaRotation
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.video.VideoOutputDetails
import com.solthariel.pawchan.media.video.VideoPlaybackPanel
import com.solthariel.pawchan.media.video.VideoTimeline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class VideoControlLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun phoneLayoutIsCompactAndHasOneRow() = verifyLayout(360)

    @Test fun wideLayoutIsCompactAndHasOneRow() = verifyLayout(480)

    @Test fun narrowLayoutCanScrollToRotationWithoutAddingAnotherRow() {
        val player = TestVideoControls()
        var rotations = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(0.5f), LocalMediaRotation provides { rotations++ }) {
                MaterialTheme { Box(Modifier.width(320.dp).testTag("panel")) { VideoPlaybackPanel(player) } }
            }
        }
        val panel = compose.onNodeWithTag("panel").fetchSemanticsNode().boundsInRoot
        assertEquals(48f, panel.height, 0.5f)
        compose.onNodeWithContentDescription("Повернуть экран").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, rotations) }
    }

    @Test fun timeLabelsAlignWithOuterControlButtons() {
        val player = TestVideoControls()
        player.status.value = player.status.value.copy(timeline = VideoTimeline(duration = 10_000))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(0.5f), LocalMediaRotation provides {}) {
                MaterialTheme { Box(Modifier.width(360.dp)) { VideoPlaybackPanel(player) } }
            }
        }
        val start = compose.onNodeWithText("0:00").fetchSemanticsNode().boundsInRoot
        val end = compose.onNodeWithText("0:10").fetchSemanticsNode().boundsInRoot
        assertEquals(bounds("Выключить звук").center.x, start.center.x, 1f)
        assertEquals(bounds("Повернуть экран").center.x, end.center.x, 1f)
    }

    @Test fun accessibleSeekStillChangesPlaybackPosition() {
        val player = TestVideoControls()
        compose.setContent { MaterialTheme { VideoPlaybackPanel(player) } }
        compose.onNodeWithContentDescription("Перемотка видео").assertIsNotEnabled()
        compose.runOnIdle {
            player.status.value = player.status.value.copy(timeline = VideoTimeline(duration = 10_000), seekable = true)
        }
        compose.onNodeWithContentDescription("Перемотка видео").assertIsEnabled().performSemanticsAction(
            SemanticsActions.SetProgress,
        ) { action -> assertTrue(action(6_000f)) }
        compose.runOnIdle { assertEquals(6_000L, player.status.value.position) }
    }

    private fun verifyLayout(width: Int) {
        val player = TestVideoControls()
        player.status.value = player.status.value.copy(output = VideoOutputDetails(hardwareDecoded = true))
        var rotations = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(0.5f), LocalMediaRotation provides { rotations++ }) {
                MaterialTheme {
                    Box(Modifier.width(width.dp).testTag("panel")) {
                        VideoPlaybackPanel(player, navigation = MediaViewerNavigation(previous = {}, next = {}))
                    }
                }
            }
        }
        val panel = compose.onNodeWithTag("panel").fetchSemanticsNode().boundsInRoot
        val descriptions =
            listOf(
                "Выключить звук",
                "Скорость воспроизведения: 1x",
                "Предыдущий файл",
                "Воспроизвести",
                "Следующий файл",
                "Аппаратный декодер. Переключить на программный",
                "Повернуть экран",
            )
        val controls = descriptions.map(::bounds)
        assertEquals("Two 48dp rows at density 0.5", 48f, panel.height, 0.5f)
        assertTrue("Play should be centered", abs(controls[3].center.x - panel.center.x) < 1f)
        for (index in controls.indices) {
            assertTrue("All controls should share a row", abs(controls[index].center.y - controls[3].center.y) < 1f)
            if (index > 0) assertTrue("Buttons must not overlap", !controls[index - 1].overlaps(controls[index]))
        }
        assertTrue("Decoder should be immediately left of rotation", abs(controls[5].right - controls[6].left) < 1f)
        compose.onNodeWithContentDescription("Картинка в картинке").assertDoesNotExist()
        compose.onNodeWithContentDescription("Аппаратный декодер. Переключить на программный").performClick()
        compose.onNodeWithContentDescription("Повернуть экран").performClick()
        compose.runOnIdle {
            assertEquals(true, player.status.value.attempt.software)
            assertEquals(1, rotations)
        }
    }

    private fun bounds(description: String): Rect = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
}
