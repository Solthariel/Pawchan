package com.solthariel.pawchan

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.RichPostText
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Rule
import org.junit.Test

class MonkeyReplyVisibilityRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingHidesOnlyTheReplyAndCanBeReversedWithoutFetchingAgain() {
        val hidden = mutableStateOf(false)
        val source = listOf(PostTextRun("@monkey вопрос\n"), PostTextRun("генерированный текст", monkeyBlock = 1))
        val model = RichPostTextUi.create(PostDocument(source))
        compose.setContent {
            ThreadTestTheme(ThemePreference.SYSTEM) {
                CompositionLocalProvider(LocalPostDisplayPreferences provides PostDisplayPreferences(hideMonkeyReplies = hidden.value)) {
                    RichPostText(model, {})
                }
            }
        }
        compose.onNodeWithText("Ответ @monkey", substring = true).assertExists()
        compose.runOnIdle { hidden.value = true }
        compose.onNodeWithText("@monkey вопрос", substring = true).assertExists()
        compose.onNodeWithText("генерированный текст", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Ответ @monkey", substring = true).assertDoesNotExist()
        compose.runOnIdle { hidden.value = false }
        compose.onNodeWithText("генерированный текст", substring = true).assertExists()
    }
}
