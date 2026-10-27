package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.ContentFilterSettings
import com.solthariel.pawchan.settings.SettingsActions
import com.solthariel.pawchan.settings.SettingsUiState
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterEditorUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptyListHasNoSearchWallAndNewRuleSurvivesParentRecomposition() {
        val state = mutableStateOf(SettingsUiState())
        compose.setContent {
            MaterialTheme {
                ContentFilterSettings(
                    state.value,
                    SettingsActions(
                        {},
                        { update -> state.value = state.value.copy(app = update(state.value.app)) },
                        {},
                    ),
                )
            }
        }
        compose.onNodeWithText("Найти правило").assertDoesNotExist()
        compose.onNodeWithContentDescription("Добавить правило").performClick()
        compose.onNodeWithText("Проверяемый текст").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Что искать").performScrollTo().performTextInput("голова дай денег")
        compose.onNodeWithText("Любое из слов").performScrollTo().performClick()
        compose.onNodeWithText("Учитывать регистр").performScrollTo().performClick()
        compose.runOnIdle {
            state.value = state.value.copy(app = state.value.app.copy(postMarkers = !state.value.app.postMarkers))
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            val save = compose.onNodeWithText("Сохранить").fetchSemanticsNode()
            !save.config.contains(SemanticsProperties.Disabled)
        }
        compose.onNodeWithText("Сохранить").performClick()
        compose.runOnIdle {
            val saved = state.value.app.contentFilters
            val rule = saved.single()
            assertEquals(ContentFilterMode.ANY_TERM, rule.mode)
            assertEquals(listOf("голова дай денег"), rule.values)
            assertTrue(rule.threads && rule.comments)
            assertTrue(rule.boards.isEmpty())
            assertTrue(rule.targets.threadIds.isEmpty())
        }
    }

    @Test
    fun onePageFormKeepsSaveAndCancelOutsideTheScrollArea() {
        val rule = newContentFilterRule(defaultBoardAddress(), "голова дай денег")
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        compose.onNodeWithText("Дополнительно").assertDoesNotExist()
        compose.onNodeWithText("Проверяемый текст").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Отмена").assertIsDisplayed()
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
        compose.onNodeWithText("Любое из слов").assertExists()
        compose.onNodeWithText("Любое из слов").performScrollTo().performClick()
        compose.onNodeWithText("Отмена").assertIsDisplayed()
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
    }

    @Test
    fun existingRuleHasOnlyDeleteCancelAndSaveWithoutDuplicationMenu() {
        val rule = newContentFilterRule(defaultBoardAddress(), "голова")
        compose.setContent {
            MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}, onDelete = {}) }
        }
        compose.onNodeWithContentDescription("Удалить правило").assertIsDisplayed()
        compose.onNodeWithText("Отмена").assertIsDisplayed()
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
        compose.onNodeWithContentDescription("Действия с правилом").assertDoesNotExist()
        compose.onNodeWithText("Дублировать").assertDoesNotExist()
    }

    @Test
    fun deletingTheRuleDoesNotInsertAnUndoRowAboveTheList() {
        val rule = newContentFilterRule(defaultBoardAddress(), "удаляемое правило")
        val initial = SettingsUiState()
        val state = mutableStateOf(initial.copy(app = initial.app.withContentFilters(listOf(rule))))
        compose.setContent {
            MaterialTheme {
                Column {
                    ContentFilterSettings(
                        state.value,
                        SettingsActions(
                            {},
                            { update -> state.value = state.value.copy(app = update(state.value.app)) },
                            {},
                        ),
                    )
                }
            }
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            compose
                .onAllNodesWithText("удаляемое правило")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("удаляемое правило").performClick()
        compose.onNodeWithContentDescription("Удалить правило").performClick()
        compose.runOnIdle {
            assertTrue(
                state
                    .value
                    .app
                    .contentFilters
                    .isEmpty(),
            )
        }
        compose.onNodeWithText("Отменить удаление").assertDoesNotExist()
        compose.onNodeWithText("Правил пока нет").assertIsDisplayed()
    }
}
