package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.threads.ThreadCounter
import com.solthariel.pawchan.threads.ThreadCounterCell
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DeletedThreadCounterRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun deletedCounterIsMutedEvenWithUnreadReplies() {
        val colors = darkColorScheme()
        compose.setContent {
            MaterialTheme(colorScheme = colors) {
                Box(Modifier.testTag("counter")) {
                    ThreadCounterCell(ThreadCounter(5, deleted = true), reply = true, width = 36.dp)
                }
            }
        }
        compose.onNodeWithText("×").assertIsDisplayed()
        val pixels = compose.onNodeWithTag("counter").captureToImage().toPixelMap()
        assertEquals(colors.surfaceVariant, pixels[pixels.width / 4, pixels.height / 4])
    }

    @Test
    fun networkFailureKeepsQuestionMarkAndAccent() {
        val colors = darkColorScheme()
        compose.setContent {
            MaterialTheme(colorScheme = colors) {
                Box(Modifier.testTag("counter")) {
                    ThreadCounterCell(ThreadCounter(error = true), reply = false, width = 36.dp)
                }
            }
        }
        compose.onNodeWithText("?").assertIsDisplayed()
        val pixels = compose.onNodeWithTag("counter").captureToImage().toPixelMap()
        assertEquals(colors.primary, pixels[pixels.width / 4, pixels.height / 4])
    }
}
