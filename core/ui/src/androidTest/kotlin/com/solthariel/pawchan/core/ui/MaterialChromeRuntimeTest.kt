package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MaterialChromeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun topBarKeeps64dpHeightAnd48dpButtonsWithoutAddingSystemInsets() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    UiTopAppBar(
                        title = { Text("Заголовок") },
                        navigationIcon = { UiToolbarButton(UiIconKind.BACK, "Назад") { clicks++ } },
                        actions = { UiToolbarButton(UiIconKind.MORE, "Меню") { clicks++ } },
                        modifier = Modifier.testTag("top-bar"),
                    )
                }
            }
        }
        compose.onNodeWithTag("top-bar").assertHeightIsEqualTo(64.dp)
        listOf("Назад", "Меню").forEach {
            compose
                .onNodeWithContentDescription(it)
                .assertWidthIsEqualTo(48.dp)
                .assertHeightIsEqualTo(48.dp)
                .performClick()
        }
        compose.onNodeWithText("Заголовок").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, clicks) }
    }

    @Test fun searchRetainsInputAndClearCallbacks() {
        var query by mutableStateOf("")
        compose.setContent {
            val focus = remember { FocusRequester() }
            MaterialTheme {
                UiToolbarSearchField(query, { query = it }, focus, Modifier.width(240.dp).testTag("query"))
            }
        }
        compose.onNodeWithTag("query").performTextInput("кот")
        compose.runOnIdle { assertEquals("кот", query) }
        compose.onNodeWithContentDescription("Очистить поиск").performClick()
        compose.runOnIdle { assertEquals("", query) }
    }
}
