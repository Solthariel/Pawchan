package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.threads.ThreadsDisplayDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThreadsDisplayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun limitsAreIndependentAndRequireConfirmation() {
        val value = mutableStateOf(NavigationPreferences(1, 20, 5))
        compose.setContent { MaterialTheme { ThreadsDisplayDialog(value.value, { value.value = it }, {}) } }
        compose
            .onNodeWithContentDescription("Ответы: количество")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(17f) }
        compose.runOnIdle { assertEquals(NavigationPreferences(1, 20, 5), value.value) }
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle { assertEquals(NavigationPreferences(1, 20, 17), value.value) }
    }

    @Test fun cancelLeavesSavedLimitsUnchanged() {
        val value = mutableStateOf(NavigationPreferences())
        val open = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                if (open.value) ThreadsDisplayDialog(value.value, { value.value = it }, { open.value = false })
            }
        }
        compose
            .onNodeWithContentDescription("Страницы: количество")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertEquals(NavigationPreferences(), value.value) }
    }
}
