package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.settings.SettingsActions
import com.solthariel.pawchan.settings.SettingsScreen
import com.solthariel.pawchan.settings.SettingsUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsToolbarUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun searchOpensInlineFindsSettingsAndBackClosesIt() {
        showSettings()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithContentDescription("Поиск настроек").performClick()
        compose.onNodeWithContentDescription("Поиск настроек").performTextInput("зеркало")
        compose.onNodeWithText("Зеркало сайта").assertIsDisplayed()
        compose.onNodeWithText("Медиа").assertDoesNotExist()
        compose.onNodeWithContentDescription("Закрыть поиск").performClick()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("Медиа").assertIsDisplayed()
    }

    @Test
    fun overflowNavigatesToSectionsAndBackReturnsToTheList() {
        showSettings()
        compose.onNodeWithText("Интерфейс").performClick()
        compose.onNodeWithText("Тема оформления").assertIsDisplayed()
        compose.onNodeWithContentDescription("Меню настроек").performClick()
        compose.onNodeWithText("Общие").performClick()
        compose.onNodeWithText("Зеркало сайта").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Медиа").assertIsDisplayed()
    }

    @Test
    fun headerRemainsFixedWhenSettingsContentScrolls() {
        showSettings(compact = true)
        val before = compose.onNodeWithContentDescription("Меню настроек").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("О программе").performScrollTo().assertIsDisplayed()
        val after = compose.onNodeWithContentDescription("Меню настроек").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
    }

    @Test
    fun tappingSelectedSettingsTabReturnsFromAutohideToTheRoot() {
        showSettings()
        compose.onNodeWithText("Автоскрытие").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Добавить правило").assertIsDisplayed()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithText("Медиа").assertIsDisplayed()
        compose.onNodeWithContentDescription("Добавить правило").assertDoesNotExist()
        compose.onNodeWithContentDescription("Поиск настроек").assertIsDisplayed()
    }

    @Test
    fun tappingSelectedSettingsTabClosesSearchAndClearsItsQuery() {
        showSettings()
        compose.onNodeWithText("Интерфейс").performClick()
        compose.onNodeWithContentDescription("Поиск настроек").performClick()
        compose.onNodeWithContentDescription("Поиск настроек").performTextInput("зеркало")
        compose.onNodeWithText("Зеркало сайта").assertIsDisplayed()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        compose.onNodeWithText("Медиа").assertIsDisplayed()
        compose.onNodeWithText("Интерфейс").performClick()
        compose.onNodeWithText("Тема оформления").assertIsDisplayed()
    }

    private fun showSettings(compact: Boolean = false) {
        compose.setContent {
            MaterialTheme {
                PreviewTestWorkspace {
                    Box(if (compact) Modifier.height(280.dp) else Modifier.fillMaxSize()) {
                        SettingsScreen(
                            SettingsUiState(),
                            SettingsActions({}, {}, {}),
                            CatalogBottomActions({}, {}, {}, {}),
                        )
                    }
                }
            }
        }
    }
}
