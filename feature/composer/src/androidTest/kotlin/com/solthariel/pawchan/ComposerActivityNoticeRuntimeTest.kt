package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.composer.ComposerActivityNotice
import com.solthariel.pawchan.composer.ComposerDraftActivity
import com.solthariel.pawchan.core.ui.AppNoticeHost
import org.junit.Rule
import org.junit.Test

class ComposerActivityNoticeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fastRestorationDoesNotFlashAndSlowImportIsDismissedWhenFinished() {
        val activity = mutableStateOf(ComposerDraftActivity.IDLE)
        compose.setContent {
            MaterialTheme {
                AppNoticeHost { ComposerActivityNotice(activity.value, null) }
            }
        }
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { activity.value = ComposerDraftActivity.RESTORING }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Восстановление черновика…").assertDoesNotExist()
        compose.onNodeWithText("Чтение вложений…").assertDoesNotExist()
        compose.runOnIdle { activity.value = ComposerDraftActivity.IDLE }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Восстановление черновика…").assertDoesNotExist()
        compose.runOnIdle { activity.value = ComposerDraftActivity.LOADING_FILES }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("Чтение вложений…").assertExists()
        compose.runOnIdle { activity.value = ComposerDraftActivity.IDLE }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("Чтение вложений…").assertDoesNotExist()
        compose.mainClock.autoAdvance = true
    }
}
