package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Выбор из длинного списка открывается без падения: диалог сам прокручивает варианты. */
class SettingsChoiceDialogRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longChoiceListOpensAndPicks() {
        var chosen = -1
        val options = List(12) { "Вариант $it" }
        compose.setContent {
            MaterialTheme { SettingsItemRow(SettingsItem.Choice("Выбор", options, 0) { chosen = it }) }
        }
        compose.onNodeWithText("Выбор").performClick()
        compose.onNodeWithText("Вариант 1").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, chosen) }
    }
}
