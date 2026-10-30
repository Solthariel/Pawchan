package com.solthariel.pawchan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.thread.PostRepliesFooter
import com.solthariel.pawchan.thread.PostTextAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostMaterialActionsRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private var density = 1f
    private var replies = 0
    private var expands = 0

    @Test fun adjacentActionsHave48dpSlotsWithoutOverlapping() {
        show()
        val first = compose.onNodeWithText("3 ответа").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Раскрыть").fetchSemanticsNode().boundsInRoot
        assertTrue(first.height >= 48f * density - 1f)
        assertTrue(first.width >= 48f * density - 1f)
        assertTrue(second.height >= 48f * density - 1f)
        assertTrue(second.left >= first.right)
        compose.onNodeWithText("3 ответа").performClick()
        compose.onNodeWithText("Раскрыть").performClick()
        compose.runOnIdle {
            assertEquals(1, replies)
            assertEquals(1, expands)
        }
    }

    @Test fun disabledActionsDoNotDispatchTouches() {
        show(enabled = false)
        compose.onNodeWithText("3 ответа").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText("Раскрыть").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(0, replies)
            assertEquals(0, expands)
        }
    }

    @Test fun largeFontCanGrowActionHeight() {
        show(scale = 2f)
        val first = compose.onNodeWithText("3 ответа").fetchSemanticsNode().boundsInRoot
        assertTrue(first.height >= 48f * density - 1f)
        compose.onNodeWithText("3 ответа").performClick()
        compose.runOnIdle { assertEquals(1, replies) }
    }

    private fun show(
        enabled: Boolean = true,
        scale: Float = 1f,
    ) {
        compose.setContent {
            val current = LocalDensity.current
            density = current.density
            CompositionLocalProvider(LocalDensity provides Density(density, scale)) {
                MaterialTheme {
                    Box(Modifier.width(300.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PostRepliesFooter("3 ответа", enabled, { replies++ }, Modifier.weight(1f))
                            PostTextAction("Раскрыть", enabled, { expands++ })
                        }
                    }
                }
            }
        }
    }
}
