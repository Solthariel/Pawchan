package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.composer.ComposerLimits
import com.solthariel.pawchan.composer.composerLimitsText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerRemainingSizeRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val remaining = mutableStateOf<Long?>(-1L)
    private var text = AnnotatedString("")
    private var error = Color.Unspecified

    @Test fun onlyNegativeSizeNumberGetsErrorColorAndOtherCountersArePreserved() {
        show()
        compose.onNodeWithText("−<0,1 МБ / 4 файла / 14997").fetchSemanticsNode()
        compose.runOnIdle {
            val style = text.spanStyles.single()
            assertEquals(0, style.start)
            assertEquals("−<0,1".length, style.end)
            assertEquals(error, style.item.color)
        }
    }

    @Test fun zeroAndUnknownSizesAreNotShownAsAnOverBudgetError() {
        show()
        compose.runOnIdle { remaining.value = 0 }
        compose.waitForIdle()
        compose.onNodeWithText("0 МБ / 4 файла / 14997").fetchSemanticsNode()
        compose.runOnIdle {
            assertTrue(text.spanStyles.isEmpty())
            remaining.value = null
        }
        compose.waitForIdle()
        compose.onNodeWithText("… МБ / 4 файла / 14997").fetchSemanticsNode()
        compose.runOnIdle { assertTrue(text.spanStyles.isEmpty()) }
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                error = MaterialTheme.colorScheme.error
                text = composerLimitsText(ComposerLimits(40 * 1024 * 1024, 4, 15000, remaining.value), 3)
                Text(text)
            }
        }
    }
}
