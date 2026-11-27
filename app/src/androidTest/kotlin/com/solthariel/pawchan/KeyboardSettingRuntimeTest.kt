package com.solthariel.pawchan

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.core.ui.FocusWhenOpened
import com.solthariel.pawchan.core.ui.LocalAutoOpenKeyboard
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** «Сразу открывать клавиатуру» и нижняя панель, которая уходит вместе с клавиатурой. */
class KeyboardSettingRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fieldTakesFocusWhenTheSettingIsOn() {
        showField(auto = true)
        compose.onNodeWithTag("field").assertIsFocused()
    }

    @Test
    fun fieldWaitsForATapWhenTheSettingIsOff() {
        showField(auto = false)
        compose.onNodeWithTag("field").assertIsNotFocused()
    }

    @Test
    fun barLeavesAtOnceWithTheKeyboardAndComesBackAfterIt() {
        var keyboard by mutableStateOf(false)
        var catalog = 0
        compose.setContent {
            MaterialTheme {
                AppNavigationWorkspace(
                    AppBottomActions({}, { catalog++ }, {}, {}),
                    AppBottomDestination.CATALOG,
                    keyboardShown = keyboard,
                ) { Text("Экран") }
            }
        }
        compose.onNodeWithContentDescription("Каталог").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        keyboard = true
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithContentDescription("Каталог").assertCountEquals(0)
        keyboard = false
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Каталог").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, catalog) }
    }

    private fun showField(auto: Boolean) {
        compose.setContent {
            CompositionLocalProvider(LocalAutoOpenKeyboard provides auto) {
                val focus = remember { FocusRequester() }
                BasicTextField(rememberTextFieldState(), Modifier.focusRequester(focus).testTag("field"))
                FocusWhenOpened(focus)
            }
        }
        compose.waitForIdle()
    }
}
