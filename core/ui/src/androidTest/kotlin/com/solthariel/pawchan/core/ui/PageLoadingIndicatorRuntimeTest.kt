package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PageLoadingIndicatorRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun indicatorsExposeDescriptionsProgressAndRequestedSizes() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Column {
                    PageLoadingIndicator("Страница")
                    PageLoadingIndicator("Меню", size = 18.dp)
                    PageLoadingIndicator("Кнопка", size = 20.dp)
                    PageLoadingIndicator("Вложение", size = 24.dp)
                }
            }
        }
        listOf("Страница" to 40, "Меню" to 18, "Кнопка" to 20, "Вложение" to 24).forEach { (label, size) ->
            val node = compose.onNodeWithContentDescription(label).assertIsDisplayed()
            node.assertWidthIsEqualTo(size.dp).assertHeightIsEqualTo(size.dp)
            assertEquals(
                ProgressBarRangeInfo.Indeterminate,
                node.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo],
            )
        }
    }

    @Test fun compactIndicatorDrawsInsideItsBoundsAndUsesProvidedColors() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                PageLoadingIndicator(
                    description = "Компактная загрузка",
                    modifier = Modifier.background(Color.Magenta).testTag("compact"),
                    size = 18.dp,
                    backgroundColor = Color.Red,
                    foregroundColor = Color.Blue,
                )
            }
        }
        val pixels = compose.onNodeWithTag("compact").captureToImage().toPixelMap()
        val corner = pixels[0, 0]
        val disc = pixels[pixels.width / 2, pixels.height / 10]
        val foreground = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(corner.red > 0.8f && corner.blue > 0.8f && corner.green < 0.2f)
        assertTrue(disc.red > 0.8f && disc.green < 0.2f && disc.blue < 0.2f)
        assertTrue(foreground.blue > 0.8f && foreground.red < 0.2f && foreground.green < 0.2f)
    }

    @Test fun indicatorRespectsTighterParentConstraints() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                PageLoadingIndicator("Ограниченный размер", modifier = Modifier.size(14.dp), size = 40.dp)
            }
        }
        compose
            .onNodeWithContentDescription("Ограниченный размер")
            .assertWidthIsEqualTo(14.dp)
            .assertHeightIsEqualTo(14.dp)
    }
}
