package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.core.ui.UiToolbarSearchTransition
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterBackupActions
import com.solthariel.pawchan.settings.ContentFilterMenuActions
import com.solthariel.pawchan.settings.ContentFilterRulesList
import com.solthariel.pawchan.settings.ContentFilterToolbar
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterAppearanceUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun titleUsesTheSameTypographyAsOtherSettingsHeaders() {
        var expected = TextStyle.Default
        compose.setContent {
            MaterialTheme {
                expected = MaterialTheme.typography.titleLarge
                val search = rememberCatalogSearchState("") {}
                val actions = ContentFilterMenuActions({}, ContentFilterBackupActions({}, {}), {})
                ContentFilterToolbar({}, search, actions)
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Автоскрытие").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        compose.runOnIdle {
            assertTrue(layouts.isNotEmpty())
            val actual = layouts.first().layoutInput.style
            assertEquals(expected.fontWeight, actual.fontWeight)
            assertEquals(expected.fontSize, actual.fontSize)
            assertEquals(expected.fontFamily, actual.fontFamily)
        }
    }

    @Test
    fun addButtonIsNotTextAndKeepsTheStandardSizeAtLargeFontScale() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f, fontScale = 1.5f)) {
                MaterialTheme {
                    val search = rememberCatalogSearchState("") {}
                    val actions = ContentFilterMenuActions({}, ContentFilterBackupActions({}, {}), {})
                    ContentFilterToolbar({}, search, actions)
                }
            }
        }
        val add = compose.onNodeWithContentDescription("Добавить правило").fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithContentDescription("Меню автоскрытия").fetchSemanticsNode().boundsInRoot
        assertEquals(96f, add.width, 0.5f)
        assertEquals(96f, add.height, 0.5f)
        assertEquals(add.width, more.width, 0.5f)
        compose.onNodeWithText("+").assertDoesNotExist()
    }

    @Test
    fun compactRulesUseCatalogOuterInsetAndSettingsInnerPadding() {
        val rule = newContentFilterRule(defaultBoardAddress(), "голова")
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 2f)) {
                MaterialTheme {
                    ContentFilterRulesList(
                        listOf(rule),
                        ContentFilterStatistics(),
                        "Правил пока нет",
                        Modifier.width(300.dp).testTag("rules"),
                    ) {}
                }
            }
        }
        val parent = compose.onNodeWithTag("rules").fetchSemanticsNode().boundsInRoot
        val text = compose.onNodeWithText("голова", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(52f, text.left - parent.left, 0.5f)
    }

    @Test
    fun emptyStateUsesSettingsBodyTypographyAndNoExtraGapAboveTheCard() {
        var expected = TextStyle.Default
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                MaterialTheme {
                    expected = MaterialTheme.typography.bodyLarge
                    ContentFilterRulesList(
                        emptyList(),
                        ContentFilterStatistics(),
                        "Правил пока нет",
                        Modifier.width(300.dp),
                    ) {}
                }
            }
        }
        val empty = compose.onNodeWithText("Правил пока нет")
        empty.assertLeftPositionInRootIsEqualTo(26.dp).assertTopPositionInRootIsEqualTo(12.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        empty.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        compose.runOnIdle {
            val actual = layouts.single().layoutInput.style
            assertEquals(expected.fontSize, actual.fontSize)
            assertEquals(expected.fontWeight, actual.fontWeight)
            assertEquals(expected.lineHeight, actual.lineHeight)
        }
    }

    @Test
    fun standardSearchTransitionHasAnIntermediateFrameAndSettlesInBothDirections() {
        val open = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                UiToolbarSearchTransition(open.value, Modifier.width(200.dp)) { active ->
                    Box(Modifier.size(200.dp, 36.dp)) { Text(if (active) "Поле поиска" else "Заголовок") }
                }
            }
        }
        compose.onNodeWithText("Заголовок").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { open.value = true }
        compose.mainClock.advanceTimeBy(64L)
        compose.onNodeWithText("Заголовок").assertExists()
        compose.onNodeWithText("Поле поиска").assertExists()
        compose.mainClock.advanceTimeBy(2_000L)
        compose.onNodeWithText("Поле поиска").assertIsDisplayed()
        compose.onNodeWithText("Заголовок").assertDoesNotExist()
        compose.runOnIdle { open.value = false }
        compose.mainClock.advanceTimeBy(2_000L)
        compose.onNodeWithText("Заголовок").assertIsDisplayed()
        compose.onNodeWithText("Поле поиска").assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }
}
