package com.solthariel.pawchan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
    fun appearanceShowsDefaultMarkersAndAlignedButtons() {
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) { ThreadAppearanceDialog(ThreadAppearance(), {}, {}) }
        }
        compose.onNodeWithText("12 sp — по умолчанию").assertIsDisplayed()
        compose.onNodeWithText("20 строк — по умолчанию").assertIsDisplayed()
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
