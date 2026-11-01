package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToolbarGeometryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toolbarButtonHasAReal48DpTarget() {
        var clicks = 0
        compose.setContent { MaterialTheme { UiToolbarButton(UiIconKind.BACK, "Назад") { clicks++ } } }
        compose
            .onNodeWithContentDescription("Назад")
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun filledAndEmptySearchKeepSameHeightAndClearRemainsReachable() {
        var value = "unchanged"
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(240.dp)) {
                    UiToolbarSearchField("", {}, remember { FocusRequester() }, Modifier.testTag("empty"))
                    UiToolbarSearchField(
                        "query",
                        { value = it },
                        remember { FocusRequester() },
                        Modifier.testTag("filled"),
                    )
                }
            }
        }
        compose.onNodeWithTag("empty").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("filled").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithContentDescription("Очистить поиск").performClick()
        compose.runOnIdle { assertEquals("", value) }
    }
}
