package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Device execution required: glyph children must never steal the row action. */
class MenuIconNavigationRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun submenuTriangleIsTrailingAndRowDispatchesOnce() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                ActionMenuItem("Копировать", { clicks++ }, navigation = MenuNavigation.FORWARD, icon = UiIconKind.COPY)
            }
        }
        val label = compose.onNodeWithText("Копировать", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val indicator = compose.onNodeWithTag("submenu-indicator", useUnmergedTree = true)
        indicator.assertHasNoClickAction()
        assertTrue(indicator.fetchSemanticsNode().boundsInRoot.left >= label.right)
        compose.onNodeWithText("Копировать").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun disabledMenuActionDoesNotDispatch() {
        var clicks = 0
        compose.setContent {
            MaterialTheme { ActionMenuItem("Удалить", { clicks++ }, enabled = false, icon = UiIconKind.DELETE) }
        }
        compose.onNodeWithText("Удалить").performClick()
        compose.runOnIdle { assertEquals(0, clicks) }
    }

    @Test fun disclosureChangesStateWhileDestinationAndSubmenuRemainSeparate() {
        val expanded = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Column {
                    UiDisclosureIndicator(expanded.value)
                    UiForwardIndicator()
                    UiSubmenuIndicator()
                    UiBackwardIndicator()
                }
            }
        }
        val indicator = compose.onNodeWithTag("disclosure-indicator", useUnmergedTree = true)
        indicator.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Свёрнуто"))
        compose.runOnIdle { expanded.value = true }
        indicator.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Развёрнуто"))
        compose.onNodeWithTag("forward-indicator", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("submenu-indicator", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("backward-indicator", useUnmergedTree = true).assertExists()
    }
}
