package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.navigation.chrome.APP_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Navigation uses M3 tabs independently of the post bubble preference. */
class BottomNavigationOutlineRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val bubbles = mutableStateOf(false)
    private val selected = mutableStateOf(AppBottomDestination.CATALOG)
    private var clicks = 0
    private val labels = listOf("Каталог", "Треды", "Доски", "Настройки")

    @Test fun darkNavigationWithPlainPostsUsesMaterialTabs() = checkMaterial(false, false)

    @Test fun darkNavigationWithBubblePostsUsesMaterialTabs() = checkMaterial(true, false)

    @Test fun lightNavigationWithPlainPostsUsesMaterialTabs() = checkMaterial(false, true)

    @Test fun lightNavigationWithBubblePostsUsesMaterialTabs() = checkMaterial(true, true)

    @Test fun changingPostStyleDoesNotMoveOrResizeNavigation() {
        show()
        val before = buttonBounds()
        compose.runOnIdle { bubbles.value = true }
        assertEquals(before, buttonBounds())
        compose.runOnIdle { bubbles.value = false }
        assertEquals(before, buttonBounds())
    }

    @Test fun allButtonsDispatchTheSameActionsInBothPostStyles() {
        show()
        repeat(2) { mode ->
            compose.runOnIdle { bubbles.value = mode == 1 }
            labels.forEach { compose.onNodeWithContentDescription(it).performClick() }
        }
        compose.runOnIdle { assertEquals(8, clicks) }
    }

    @Test fun changingDestinationUpdatesTheSelectedMaterialTab() {
        show()
        compose.onNodeWithContentDescription("Каталог").assertIsSelected()
        compose.onNodeWithContentDescription("Настройки").assertIsNotSelected()
        compose.runOnIdle { selected.value = AppBottomDestination.SETTINGS }
        compose.onNodeWithContentDescription("Каталог").assertIsNotSelected()
        compose.onNodeWithContentDescription("Настройки").assertIsSelected()
    }

    private fun checkMaterial(
        bubble: Boolean,
        light: Boolean,
    ) {
        bubbles.value = bubble
        show(light)
        compose.onNodeWithTag("navigation-bar").assertHeightIsEqualTo(APP_BOTTOM_BAR_HEIGHT)
        labels.forEach { label ->
            compose.onNodeWithText(label).assertIsDisplayed()
            compose
                .onNodeWithContentDescription(label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        }
        val bounds = buttonBounds()
        val minimum = with(compose.density) { 48.dp.toPx() }
        bounds.forEach { assertTrue(it.width >= minimum && it.height >= minimum) }
        bounds.zipWithNext().forEach { (left, right) -> assertTrue(left.right <= right.left + 1f) }
    }

    private fun show(light: Boolean = false) {
        compose.setContent {
            MaterialTheme(colorScheme = if (light) lightColorScheme() else darkColorScheme()) {
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(bubblePosts = bubbles.value),
                ) {
                    Box(Modifier.width(320.dp).consumeWindowInsets(WindowInsets.navigationBars)) {
                        AppBottomBar(
                            AppBottomActions({ clicks++ }, { clicks++ }, { clicks++ }, { clicks++ }),
                            selected.value,
                            modifier = Modifier.testTag("navigation-bar"),
                        )
                    }
                }
            }
        }
    }

    private fun buttonBounds(): List<Rect> =
        labels.map {
            compose.onNodeWithContentDescription(it).fetchSemanticsNode().boundsInRoot
        }
}
