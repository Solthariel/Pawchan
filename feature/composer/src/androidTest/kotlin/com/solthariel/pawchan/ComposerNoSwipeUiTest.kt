package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.solthariel.pawchan.composer.ComposerFullScreenHost
import org.junit.Rule
import org.junit.Test

class ComposerNoSwipeUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun formOnlyOpensFromTheButtonNotFromSwipe() {
        val visible = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                ComposerFullScreenHost(visible.value, { visible.value = false }, content = {
                    Box(Modifier.fillMaxSize().testTag("catalog-content")) {
                        TextButton(onClick = { visible.value = true }) { Text("Написать тест") }
                    }
                }, form = {
                    TextButton(onClick = { visible.value = false }) { Text("Закрыть полную форму") }
                })
            }
        }
        compose.onNodeWithTag("catalog-content").performTouchInput { swipeLeft() }
        compose.onNodeWithText("Закрыть полную форму").assertDoesNotExist()
        compose.onNodeWithText("Написать тест").performClick()
        compose.onNodeWithText("Закрыть полную форму").assertIsDisplayed().performClick()
        compose.onNodeWithText("Написать тест").assertIsDisplayed()
    }
}
