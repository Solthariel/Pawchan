package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.MediaChromeState
import com.solthariel.pawchan.media.MediaViewerChrome
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoChromeOverlayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun panelTintIsTranslucentWhileCenterRemainsUndimmed() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current
            val motion = remember { ImageViewerMotion(scope, context, MediaChromeState()) }
            Box(Modifier.size(240.dp).background(Color.Red).testTag("stage")) {
                MediaViewerChrome(
                    motion,
                    bottom = { Box(Modifier.fillMaxWidth().height(60.dp)) },
                    blackChrome = true,
                ) { Box(Modifier.fillMaxWidth().height(60.dp)) }
            }
        }
        val image = compose.onNodeWithTag("stage").captureToImage().toPixelMap()
        assertEquals(0.4f, image[image.width / 2, image.height / 10].red, 0.05f)
        assertEquals(1f, image[image.width / 2, image.height / 2].red, 0.05f)
        assertEquals(0.4f, image[image.width / 2, image.height * 8 / 10].red, 0.05f)
    }
}
