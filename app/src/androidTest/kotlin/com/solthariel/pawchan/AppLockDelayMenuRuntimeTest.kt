package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.solthariel.pawchan.domain.security.AppLockDelay
import com.solthariel.pawchan.security.AppLockSettingsContent
import com.solthariel.pawchan.security.AppLockSettingsState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLockDelayMenuRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sixHourChoiceIsSelectableAndReflectedInTheSettingsRow() {
        var state by mutableStateOf(AppLockSettingsState(enabled = true))
        compose.setContent {
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AppLockSettingsContent(state, false, { state = state.copy(delay = it) }, {})
                }
            }
        }
        compose.onNodeWithText("Запрашивать PIN после отсутствия").performScrollTo().performClick()
        compose.onNodeWithText("Через 15 минут").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Через 3 часа").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Через 6 часов").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(AppLockDelay.HOURS_6, state.delay) }
        compose.onNodeWithText("Через 6 часов").assertIsDisplayed()
    }
}
