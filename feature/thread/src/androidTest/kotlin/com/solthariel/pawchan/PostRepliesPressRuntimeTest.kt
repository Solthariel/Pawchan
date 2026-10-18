package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.PostRepliesFooter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostRepliesPressRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pressedRippleLeavesCornersClearWithoutChangingBounds() {
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                Box(Modifier.background(MaterialTheme.colorScheme.surface).padding(8.dp)) {
                    PostRepliesFooter("Ответов: 3", true, {}, Modifier.fillMaxWidth())
                }
            }
        }
        val replies = compose.onNodeWithText("Ответов: 3")
        val bounds = replies.fetchSemanticsNode().boundsInRoot
        val before = replies.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        replies.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(400)
        val pressed = replies.captureToImage().toPixelMap()
        assertEquals(bounds, replies.fetchSemanticsNode().boundsInRoot)
        assertEquals(before[0, 0], pressed[0, 0])
        assertEquals(before[before.width - 1, 0], pressed[pressed.width - 1, 0])
        replies.performTouchInput { up() }
        compose.mainClock.autoAdvance = true
    }
}
