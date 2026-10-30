package com.solthariel.pawchan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToolbarButtonMaterialRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toolbarActionHasButtonRoleAndDoesNotFireWhileDisabled() {
        var enabled by mutableStateOf(true)
        var clicks = 0
        compose.setContent {
            MaterialTheme { UiToolbarButton(UiIconKind.REFRESH, "Обновить", enabled = enabled) { clicks++ } }
        }
        val button = compose.onNodeWithContentDescription("Обновить")
        button.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)).performClick()
        compose.runOnIdle {
            assertEquals(1, clicks)
            enabled = false
        }
        button.assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }
}
