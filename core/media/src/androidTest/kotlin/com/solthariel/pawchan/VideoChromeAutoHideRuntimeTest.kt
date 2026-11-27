package com.solthariel.pawchan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.MediaChromeState
import com.solthariel.pawchan.media.video.VideoPlaybackStatus
import com.solthariel.pawchan.media.video.VideoStage
import com.solthariel.pawchan.media.video.rememberVideoChromeActivity
import com.solthariel.pawchan.media.video.videoChromeActivity
import org.junit.Rule
import org.junit.Test

class VideoChromeAutoHideRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun controlsHideDuringPlaybackButRemainVisibleWhenPaused() {
        compose.mainClock.autoAdvance = false
        var playing by mutableStateOf(true)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val chrome = remember { MediaChromeState() }
            val motion = remember { ImageViewerMotion(scope, context, chrome) }
            val activity = rememberVideoChromeActivity(motion, VideoPlaybackStatus(stage = VideoStage.READY, playing = playing))
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("video")
                    .videoChromeActivity(activity)
                    .clickable { motion.toggleControls() },
            ) {
                if (motion.controlsVisible) Text("controls")
            }
        }
        compose.mainClock.advanceTimeBy(4_800)
        compose.waitForIdle()
        compose.onNodeWithText("controls").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(400)
        compose.waitForIdle()
        compose.onNodeWithText("controls").assertDoesNotExist()
        compose.onNodeWithTag("video").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        compose.onNodeWithText("controls").assertIsDisplayed()
        compose.runOnIdle { playing = false }
        compose.mainClock.advanceTimeBy(6_000)
        compose.onNodeWithText("controls").assertIsDisplayed()
    }
}
