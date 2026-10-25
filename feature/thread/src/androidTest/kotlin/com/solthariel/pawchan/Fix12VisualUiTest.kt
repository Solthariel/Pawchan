package com.solthariel.pawchan

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.PostMenuDialog
import com.solthariel.pawchan.thread.PostMenuOptions
import com.solthariel.pawchan.thread.ThreadAppearance
import com.solthariel.pawchan.thread.ThreadAppearanceDialog
import org.junit.Rule
import org.junit.Test

class Fix12VisualUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun appearanceDefaultsCanBeChangedAndRestored() {
        val appearance = mutableStateOf(ThreadAppearance())
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                ThreadAppearanceDialog(appearance.value, { appearance.value = it }, {})
            }
        }
        compose.onNodeWithText("Текст поста: 12 sp").assertIsDisplayed()
        compose.onNodeWithText("Сворачивать после: 20 строк").assertIsDisplayed()
        val sliders = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress))
        sliders.assertCountEquals(2)
        sliders[0].performSemanticsAction(SemanticsActions.SetProgress) { it(14f) }
        sliders[1].performSemanticsAction(SemanticsActions.SetProgress) { it(25f) }
        compose.onNodeWithText("Текст поста: 14 sp").assertIsDisplayed()
        compose.onNodeWithText("Сворачивать после: 25 строк").assertIsDisplayed()
        compose.onNodeWithText("По умолчанию").performClick()
        compose.onNodeWithText("Текст поста: 12 sp").assertIsDisplayed()
        compose.onNodeWithText("Сворачивать после: 20 строк").assertIsDisplayed()
        saveThreadChromeScreenshot("fix12-appearance")
    }

    @Test
    fun fullPostMenuScreenshot() {
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                PostMenuDialog(PostMenuOptions(true, false, true, true, false, hasLink = true), {}, {})
            }
        }
        compose.onNodeWithText("Скрыть").assertIsDisplayed()
        saveThreadChromeScreenshot("fix12-post-menu")
    }
}
