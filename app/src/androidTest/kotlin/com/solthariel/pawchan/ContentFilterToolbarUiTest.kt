package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.rememberCatalogSearchState
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterBackupActions
import com.solthariel.pawchan.settings.ContentFilterMenuActions
import com.solthariel.pawchan.settings.ContentFilterSettings
import com.solthariel.pawchan.settings.ContentFilterToolbar
import com.solthariel.pawchan.settings.SettingsActions
import com.solthariel.pawchan.settings.SettingsUiState
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterToolbarUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun toolbarHasEquallySizedSearchAddAndOverflowButtonsInThatOrder() {
        compose.setContent {
            MaterialTheme {
                val search = rememberCatalogSearchState("") {}
                val actions = ContentFilterMenuActions({}, ContentFilterBackupActions({}, {}), {})
                ContentFilterToolbar({}, search, actions)
            }
        }
        compose.onNodeWithContentDescription("Назад").assertLeftPositionInRootIsEqualTo(12.dp)
        listOf("Назад", "Поиск правил", "Добавить правило", "Меню автоскрытия").forEach { description ->
            compose
                .onNodeWithContentDescription(description)
                .assertWidthIsEqualTo(36.dp)
                .assertHeightIsEqualTo(36.dp)
                .assertTopPositionInRootIsEqualTo(9.dp)
        }
        val add = compose.onNodeWithContentDescription("Добавить правило").fetchSemanticsNode().boundsInRoot
        val search = compose.onNodeWithContentDescription("Поиск правил").fetchSemanticsNode().boundsInRoot
        val more = compose.onNodeWithContentDescription("Меню автоскрытия").fetchSemanticsNode().boundsInRoot
        assertTrue(search.right <= add.left)
        assertTrue(add.right <= more.left)
        assertEquals(add.height, search.height, 0.5f)
        assertEquals(search.height, more.height, 0.5f)
        assertEquals(add.top, more.top, 0.5f)
        compose.onNodeWithContentDescription("Меню автоскрытия").performClick()
        listOf("Все", "ОП", "Ответы", "Выключенные").forEach { label ->
            compose.onNodeWithText(label).assertDoesNotExist()
        }
        compose.onNodeWithText("Сохранить правила в TXT").assertIsDisplayed()
        compose.onNodeWithText("Восстановить правила из TXT").assertIsDisplayed()
        compose.onNodeWithText("Статистика").assertIsDisplayed()
        compose.onNodeWithText("Выводить скрытые треды").assertDoesNotExist()
        compose.onNodeWithText("Поиск правил").assertDoesNotExist()
    }

    @Test
    fun actionButtonsUseCatalogAxesAndTheStandardNineDpVerticalInset() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                MaterialTheme {
                    Box(Modifier.width(300.dp)) {
                        val search = rememberCatalogSearchState("") {}
                        val actions = ContentFilterMenuActions({}, ContentFilterBackupActions({}, {}), {})
                        ContentFilterToolbar({}, search, actions)
                    }
                }
            }
        }
        val positions =
            mapOf(
                "Назад" to 12.dp,
                "Поиск правил" to 180.dp,
                "Добавить правило" to 216.dp,
                "Меню автоскрытия" to 252.dp,
            )
        positions.forEach { (description, left) ->
            compose
                .onNodeWithContentDescription(description)
                .assertLeftPositionInRootIsEqualTo(left)
                .assertTopPositionInRootIsEqualTo(9.dp)
                .assertWidthIsEqualTo(36.dp)
                .assertHeightIsEqualTo(36.dp)
        }
    }

    @Test
    fun nativeSearchFiltersRulesAndBackClearsAndClosesIt() {
        val initial = SettingsUiState()
        val board = defaultBoardAddress()
        val rules = listOf(newContentFilterRule(board, "голова"), newContentFilterRule(board, "хлеб"))
        val state = initial.copy(app = initial.app.withContentFilters(rules))
        compose.setContent {
            MaterialTheme { Column { ContentFilterSettings(state, SettingsActions({}, {}, {})) } }
        }
        compose.onNodeWithContentDescription("Поиск правил").performClick()
        compose.onNodeWithContentDescription("Найти правило").performTextInput("хлеб")
        compose.onNodeWithContentDescription("Найти правило").assertTextEquals("хлеб")
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("голова").fetchSemanticsNodes().isEmpty() &&
                compose.onAllNodes(hasText("хлеб") and !hasSetTextAction()).fetchSemanticsNodes().size == 1
        }
        val result = hasText("хлеб") and !hasSetTextAction()
        compose.onAllNodes(result).assertCountEquals(1)
        compose.onNode(result).assertIsDisplayed()
        compose.onNodeWithContentDescription("Поиск правил").assertDoesNotExist()
        compose.onNodeWithContentDescription("Закрыть поиск").performClick()
        compose.onNodeWithContentDescription("Найти правило").assertDoesNotExist()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText("голова").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("голова").assertIsDisplayed()
    }
}
