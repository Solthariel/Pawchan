package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageQualityControlRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun presetsAndManualInputShareOneEqualCellRow() {
        val quality = mutableStateOf(100)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp)) { ImageQualityControl(quality.value, true) { quality.value = it } }
            }
        }
        val cells =
            listOf("75%", "85%", "95%", "100%").map {
                compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot
            }
        cells.forEach {
            assertEquals(cells.first().top, it.top, 1f)
            assertEquals(cells.first().width, it.width, 1f)
            assertEquals(cells.first().height, it.height, 1f)
        }
        assertTrue(cells.zipWithNext().all { (left, right) -> left.right < right.left })
        compose.onNodeWithText("85%").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(85, quality.value) }
        compose.onNodeWithContentDescription("Ввести качество JPEG").performClick()
        compose.onNodeWithText("Значение").performTextReplacement("93")
        compose.onNodeWithTag("number-confirm").performClick()
        compose.onNodeWithText("93%").assertIsSelected()
        compose.runOnIdle { assertEquals(93, quality.value) }
    }

    @Test fun invalidInputAndCancelDoNotChangeQuality() {
        val quality = mutableStateOf(100)
        compose.setContent { MaterialTheme { ImageQualityControl(quality.value, true) { quality.value = it } } }
        compose.onNodeWithContentDescription("Ввести качество JPEG").performClick()
        compose.onNodeWithText("Значение").performTextReplacement("0")
        compose.onNodeWithTag("number-confirm").assertIsNotEnabled()
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertEquals(100, quality.value) }
        compose.onNodeWithText("100%").assertIsSelected()
    }

    @Test fun disabledControlCannotOpenManualEntry() {
        compose.setContent { MaterialTheme { ImageQualityControl(100, false) {} } }
        compose.onNodeWithContentDescription("Ввести качество JPEG").assertIsNotEnabled()
        compose.onNodeWithText("75%").assertIsNotEnabled()
    }
}
