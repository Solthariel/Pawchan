package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.solthariel.pawchan.domain.security.AppLockDelay
import com.solthariel.pawchan.domain.security.appLockDelayChoices
import com.solthariel.pawchan.security.AppLockSettingsContent
import com.solthariel.pawchan.security.AppLockSettingsState
import com.solthariel.pawchan.security.LockAction
import com.solthariel.pawchan.security.label
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SecuritySettingsLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun disabledLockUsesTheSameTextInsetAsOtherSettings() {
        var action: LockAction? = null
        compose.setContent {
            MaterialTheme {
                Column {
                    SettingsItemRow(SettingsItem.Toggle("Обычная настройка", "Описание", false) {})
                    AppLockSettingsContent(AppLockSettingsState(), false) { action = it }
                }
            }
        }
        val normalNode = compose.onNodeWithText("Обычная настройка", useUnmergedTree = true)
        val securityNode = compose.onNodeWithText("Блокировка приложения", useUnmergedTree = true)
        val normal = normalNode.fetchSemanticsNode().boundsInRoot
        val security = securityNode.fetchSemanticsNode().boundsInRoot
        assertEquals(normal.left, security.left, 1f)
        compose.onNode(hasText("Блокировка приложения") and isToggleable()).performClick()
        compose.runOnIdle { assertEquals(LockAction.ENABLE, action) }
    }

    @Test fun enabledLockAndFingerprintUseStandardSettingRows() {
        var action: LockAction? = null
        compose.setContent {
            MaterialTheme {
                AppLockSettingsContent(
                    AppLockSettingsState(enabled = true, biometric = true),
                    true,
                ) { action = it }
            }
        }
        compose.onNode(hasText("Блокировка приложения") and isToggleable()).assertIsOn()
        compose.onNode(hasText("Вход по отпечатку") and isToggleable()).assertIsOn().performClick()
        compose.runOnIdle { assertEquals(LockAction.BIOMETRIC_DISABLE, action) }
        compose.onNodeWithText("запрещены снимки экрана", substring = true).assertDoesNotExist()
    }

    @Test fun delayChoiceOffersAndAppliesAllSevenIntervals() {
        val state = mutableStateOf(AppLockSettingsState(enabled = true))
        compose.setContent {
            MaterialTheme {
                AppLockSettingsContent(
                    state.value,
                    false,
                    onDelay = { delay ->
                        state.value = state.value.copy(delay = delay)
                    },
                    onEdit = {},
                )
            }
        }
        appLockDelayChoices(AppLockDelay.MINUTE_1).forEach { delay ->
            compose.onNodeWithText("Запрашивать PIN после отсутствия").performClick()
            compose.onNode(hasText(delay.label) and hasAnyAncestor(isDialog())).performScrollTo().performClick()
            compose.runOnIdle { assertEquals(delay, state.value.delay) }
        }
    }
}
