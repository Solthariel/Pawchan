package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.threads.ThreadCounter
import com.solthariel.pawchan.threads.ThreadsLinkRow
import com.solthariel.pawchan.threads.ThreadsSection
import com.solthariel.pawchan.threads.ThreadsSectionHeader
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NavigationIndicatorsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun disclosureAndForwardHaveEqualSizesAndOppositeLabelPositions() {
        compose.setContent {
            MaterialTheme {
                Column {
                    ThreadsSectionHeader(ThreadsSection.PAGES, false, ThreadCounter(), {})
                    ThreadsLinkRow(UiIconKind.DOCUMENT, "История", null, {})
                }
            }
        }
        val disclosure = compose.onNodeWithTag("disclosure-indicator", useUnmergedTree = true)
        val forward = compose.onNodeWithTag("forward-indicator", useUnmergedTree = true)
        disclosure.assertWidthIsEqualTo(20.dp).assertHeightIsEqualTo(20.dp)
        forward.assertWidthIsEqualTo(20.dp).assertHeightIsEqualTo(20.dp)
        val heading = compose.onNodeWithText("Страницы", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val history = compose.onNodeWithText("История", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(disclosure.fetchSemanticsNode().boundsInRoot.right <= heading.left)
        assertTrue(forward.fetchSemanticsNode().boundsInRoot.left >= history.right)
    }
}
