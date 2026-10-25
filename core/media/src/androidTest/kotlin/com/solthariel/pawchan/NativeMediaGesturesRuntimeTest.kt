package com.solthariel.pawchan

import android.view.ViewConfiguration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.solthariel.pawchan.media.ImageViewerMotion
import com.solthariel.pawchan.media.ImageViewport
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.mediaViewerGestures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NativeMediaGesturesRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var motion: ImageViewerMotion
    private var menu = false
    private var page = false
    private var toolbarClick = false

    @Test
    fun confirmedTapTogglesChromeAndLongPressOnlyOpensMenu() {
        fixture()
        compose.onNodeWithTag("canvas").performTouchInput { click(center) }
        compose.waitUntil(2000) { !motion.controlsVisible }
        compose.runOnIdle { assertEquals(false, menu) }
        // GestureDetector's long press is a real Handler timeout, not Compose's animation clock.
        // Keep the pointer down until the native callback; synthetic longClick sends UP too soon.
        compose.onNodeWithTag("canvas").performTouchInput { down(center) }
        try {
            compose.waitUntil(ViewConfiguration.getLongPressTimeout().toLong() + 1000) { menu }
        } finally {
            compose.onNodeWithTag("canvas").performTouchInput { up() }
        }
        compose.runOnIdle {
            assertTrue(menu)
            assertTrue(!motion.controlsVisible)
        }
    }

    @Test
    fun doubleTapZoomDoesNotAlsoToggleChrome() {
        fixture()
        compose.onNodeWithTag("canvas").performTouchInput { doubleClick(center) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(motion.transform.scale > 1f)
            assertTrue(motion.controlsVisible)
        }
    }

    @Test
    fun pageSwipeInvokesNextOnlyAfterSettlement() {
        fixture()
        compose.onNodeWithTag("canvas").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(page) }
    }

    @Test
    fun toolbarIsNotInterceptedByCanvasGestureOwner() {
        fixture()
        compose.onNodeWithText("Toolbar action").performTouchInput { click(center) }
        compose.runOnIdle {
            assertTrue(toolbarClick)
            assertEquals(1f, motion.transform.scale, 0f)
        }
    }

    private fun fixture() {
        compose.setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            motion = remember { ImageViewerMotion(scope, context) }
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .testTag("canvas")
                        .onSizeChanged {
                            motion.resize(ImageViewport(it.width.toFloat(), it.height.toFloat(), 2400f, 3200f))
                        }.mediaViewerGestures(motion, MediaViewerNavigation(next = { page = true }), {}, { menu = true }),
                )
                Button(onClick = { toolbarClick = true }) { Text("Toolbar action") }
            }
        }
    }
}
