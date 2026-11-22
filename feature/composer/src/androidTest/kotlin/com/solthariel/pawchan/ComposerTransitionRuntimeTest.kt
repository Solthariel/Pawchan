package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.composer.ComposerFullScreenHost
import com.solthariel.pawchan.composer.StatefulComposerFullScreenHost
import org.junit.Rule
import org.junit.Test

class ComposerTransitionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun closingKeepsFormUntilExitFinishesAndThenRestoresUnderlyingContent() {
        val visible = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                ComposerFullScreenHost(visible.value, { visible.value = false }, content = { Text("Каталог") }) {
                    Text("Форма создания")
                }
            }
        }
        compose.onNodeWithText("Форма создания").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { visible.value = false }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Форма создания").assertExists()
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithText("Форма создания").assertDoesNotExist()
        compose.onNodeWithText("Каталог").assertIsDisplayed()
    }

    @Test
    fun closingKeepsTheLastVisibleFieldsUntilTheWholeFormHasFaded() {
        val form = mutableStateOf(TransitionForm(true, "Черновик", "Капча"))
        compose.setContent {
            MaterialTheme {
                val state = form.value
                StatefulComposerFullScreenHost(state.visible, state, {}, content = { Text("Каталог") }) { displayed ->
                    Text(displayed.comment)
                    Text(displayed.captcha)
                }
            }
        }
        compose.onNodeWithText("Капча").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { form.value = TransitionForm(false, "Пустой черновик", "Нет капчи") }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Черновик").assertExists()
        compose.onNodeWithText("Капча").assertExists()
        compose.onNodeWithText("Нет капчи").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(250)
        compose.onNodeWithText("Черновик").assertDoesNotExist()
    }

    @Test
    fun reopeningDuringExitShowsTheNewStateInsteadOfTheOldSnapshot() {
        val form = mutableStateOf(TransitionForm(true, "Первый", "Старая капча"))
        compose.setContent {
            MaterialTheme {
                val state = form.value
                StatefulComposerFullScreenHost(state.visible, state, {}, content = {}) { displayed ->
                    Text(displayed.comment)
                }
            }
        }
        compose.onNodeWithText("Первый").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { form.value = TransitionForm(false, "", "") }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { form.value = TransitionForm(true, "Второй", "Новая капча") }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("Второй").assertExists()
        compose.onNodeWithText("Первый").assertDoesNotExist()
    }

    private data class TransitionForm(
        val visible: Boolean,
        val comment: String,
        val captcha: String,
    )
}
