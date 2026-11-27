package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.domain.settings.BottomBarButton
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.LocalBottomBarOrder
import com.solthariel.pawchan.settings.BottomBarOrderDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Порядок кнопок нижней панели: у всех после обновления прежний, сохранённый рисуется слева направо. */
class BottomBarOrderRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private val labels = listOf("Каталог", "Треды", "Доски", "Настройки", "Написать")

    @Test
    fun existingUsersKeepTheCurrentOrder() {
        compose.setContent {
            MaterialTheme { AppBottomBar(AppBottomActions({}, {}, {}, {}, {}), AppBottomDestination.CATALOG) }
        }
        assertEquals(labels, labelsLeftToRight())
    }

    @Test
    fun savedOrderIsDrawnLeftToRightAndClicksStayWithTheirButton() {
        var settings = 0
        val order =
            listOf(
                BottomBarButton.SETTINGS,
                BottomBarButton.THREADS,
                BottomBarButton.CATALOG,
                BottomBarButton.WRITE,
                BottomBarButton.BOARDS,
            )
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalBottomBarOrder provides order) {
                    AppBottomBar(AppBottomActions({}, {}, {}, { settings++ }, {}), AppBottomDestination.CATALOG)
                }
            }
        }
        assertEquals(listOf("Настройки", "Треды", "Каталог", "Написать", "Доски"), labelsLeftToRight())
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.runOnIdle { assertEquals(1, settings) }
    }

    @Test
    fun arrowsMoveButtonsAndEdgesAreInactive() {
        var saved: List<BottomBarButton>? = null
        compose.setContent { MaterialTheme { BottomBarOrderDialog(BottomBarButton.entries, { saved = it }, {}) } }
        compose.onNodeWithContentDescription("Выше: Каталог").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Ниже: Написать").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Выше: Треды").assertIsEnabled().performClick()
        compose.onNodeWithText("Готово").performClick()
        compose.runOnIdle {
            assertEquals(
                listOf(
                    BottomBarButton.THREADS,
                    BottomBarButton.CATALOG,
                    BottomBarButton.BOARDS,
                    BottomBarButton.SETTINGS,
                    BottomBarButton.WRITE,
                ),
                saved,
            )
        }
    }

    @Test
    fun resetReturnsTheDefaultOrder() {
        var saved: List<BottomBarButton>? = null
        compose.setContent {
            MaterialTheme { BottomBarOrderDialog(BottomBarButton.entries.reversed(), { saved = it }, {}) }
        }
        compose.onNodeWithText("Сбросить").performClick()
        compose.onNodeWithText("Готово").performClick()
        compose.runOnIdle { assertEquals(BottomBarButton.entries, saved) }
    }

    private fun labelsLeftToRight(): List<String> =
        labels.sortedBy {
            compose
                .onNodeWithContentDescription(it)
                .fetchSemanticsNode()
                .boundsInRoot.left
        }
}
