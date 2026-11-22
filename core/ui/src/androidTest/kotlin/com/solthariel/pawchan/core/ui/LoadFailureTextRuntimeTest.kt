package com.solthariel.pawchan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoadFailureTextRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun failedRefreshUsesTheSameTextForNetworkFailure() {
        compose.setContent {
            MaterialTheme { AppNoticeHost { RefreshFailureNotice(RefreshFailure(), {}) } }
        }
        compose.onNodeWithText("Ошибка при загрузке данных").assertIsDisplayed()
        compose.onNodeWithText("Повторить").assertDoesNotExist()
    }

    @Test fun failedRefreshUsesTheSameTextWhenRateLimited() {
        compose.setContent {
            MaterialTheme { AppNoticeHost { RefreshFailureNotice(RefreshFailure(rateLimited = true), {}) } }
        }
        compose.onNodeWithText("Ошибка при загрузке данных").assertIsDisplayed()
    }

    @Test fun protectionActionRemainsAvailable() {
        var opened = ""
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    RefreshFailureNotice(RefreshFailure(challengeUrl = "https://example.com")) { opened = it }
                }
            }
        }
        compose.onNodeWithText("Ошибка при загрузке данных").assertIsDisplayed()
        compose.onNodeWithText("Пройти проверку").performClick()
        compose.runOnIdle { assertEquals("https://example.com", opened) }
    }

    @Test fun initialFailureUsesTheSameText() {
        compose.setContent { MaterialTheme { LoadErrorPane() } }
        compose.onNodeWithText("Ошибка при загрузке данных").assertIsDisplayed()
    }

    @Test fun initialFailureUsesTheSameTextWhenRateLimited() {
        compose.setContent { MaterialTheme { LoadErrorPane(rateLimited = true) } }
        compose.onNodeWithText("Ошибка при загрузке данных").assertIsDisplayed()
    }
}
