package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.ImageTransform
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.ImageViewport
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.mediaViewerGestures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageViewerGestureRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var motion: ImageViewerMotion
    private var next = 0
    private var closed = 0

    @Test
    fun doubleTapZoomCanBeFollowedByPanAndReset() {
        compose.setContent { Fixture() }
        compose.onNodeWithTag("gesture-image").performTouchInput { doubleClick(center) }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(motion.transform.scale > 1f) }
        compose.onNodeWithTag("gesture-image").performTouchInput {
            swipe(center, center + Offset(70f, 30f), 300)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(motion.transform.x > 0f)
            assertEquals(0, next)
            assertEquals(0, closed)
        }
        compose.onNodeWithTag("gesture-image").performTouchInput { doubleClick(center) }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(ImageTransform(), motion.transform) }
    }

    @Test
    fun horizontalSwipePagesAndVerticalSwipeCloses() {
        compose.setContent { Fixture() }
        compose.onNodeWithTag("gesture-image").performTouchInput {
            swipe(Offset(width * 0.8f, height / 2f), Offset(width * 0.2f, height / 2f), 300)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, next) }
        compose.onNodeWithTag("gesture-image").performTouchInput {
            swipe(Offset(width / 2f, height * 0.2f), Offset(width / 2f, height * 0.8f), 300)
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Composable
    private fun Fixture() {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        motion = remember { ImageViewerMotion(scope, context) }
        val navigation =
            MediaViewerNavigation(next = {
                next++
                motion.reset()
            }, total = 2)
        Box(
            Modifier
                .size(300.dp)
                .testTag("gesture-image")
                .onSizeChanged {
                    motion.resize(ImageViewport(it.width.toFloat(), it.height.toFloat(), it.width * 4f, it.height * 4f))
                }.mediaViewerGestures(motion, navigation, { closed++ }, {}),
        )
    }
}
