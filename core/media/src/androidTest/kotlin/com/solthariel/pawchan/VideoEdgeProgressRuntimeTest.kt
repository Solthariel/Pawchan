package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.video.VideoEdgeProgress
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoStage
import com.solthariel.pawchan.media.video.VideoTimeline
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class VideoEdgeProgressRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    @Test
    fun hiddenProgressIsFullWidthThinAndHasNoClickOrSeekActions() {
        val visible = mutableStateOf(false)
        var bottomInset = 0f
        var usableWidth = 240.dp
        compose.setContent {
            MaterialTheme {
                val density = LocalDensity.current
                val direction = LocalLayoutDirection.current
                val bars = WindowInsets.navigationBarsIgnoringVisibility
                bottomInset = bars.getBottom(density).toFloat()
                usableWidth = 240.dp -
                    with(density) {
                        (bars.getLeft(density, direction) + bars.getRight(density, direction)).toDp()
                    }
                Box(Modifier.size(240.dp).testTag("video-progress-container")) {
                    VideoEdgeProgress(
                        VideoPlaybackStatus(stage = VideoStage.READY, timeline = VideoTimeline(25, 100)),
                        visible.value,
                        MaterialTheme.colorScheme,
                        Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
        val line = compose.onNodeWithTag("video-edge-progress")
        line.assertWidthIsEqualTo(usableWidth).assertHeightIsEqualTo(2.dp)
        val container = compose.onNodeWithTag("video-progress-container").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertEquals(container.bottom - bottomInset, line.fetchSemanticsNode().boundsInRoot.bottom, 1f)
        val semantics = line.fetchSemanticsNode().config
        assertFalse(semantics.contains(SemanticsActions.OnClick))
        assertFalse(semantics.contains(SemanticsActions.SetProgress))
        compose.runOnIdle { visible.value = true }
        line.assertDoesNotExist()
        compose.runOnIdle { visible.value = false }
        line.assertExists()
    }
}
