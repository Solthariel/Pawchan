package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.solthariel.pawchan.domain.filter.ContentFilterBoardStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterHit
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterSettings
import com.solthariel.pawchan.settings.SettingsActions
import com.solthariel.pawchan.settings.SettingsUiState
import com.solthariel.pawchan.settings.newContentFilterRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterStatisticsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun numericOnlyCounterIsAtTheRightOfTheRule() {
        showSettings()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose
                .onAllNodesWithText("голова")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        val label = compose.onNodeWithText("голова", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val badge =
            compose
                .onNodeWithContentDescription("Срабатываний: 12", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        assertTrue(label.right <= badge.left)
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("Срабатываний: 12").assertDoesNotExist()
    }

    @Test
    fun statisticsShowBoardsAndResetRequiresConfirmation() {
        val repository = showSettings()
        compose.onNodeWithContentDescription("Меню автоскрытия").performClick()
        compose.onNodeWithText("Статистика").performClick()
        compose.onNodeWithText("Одно сообщение учитывается", substring = true).assertDoesNotExist()
        compose
            .onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
                    hasAnyAncestor(isDialog()),
                useUnmergedTree = true,
            ).assertCountEquals(1)
        compose.onNodeWithText("Всего срабатываний: 12").assertIsDisplayed()
        compose.onAllNodesWithText("ОП-посты: 3").assertCountEquals(2)
        compose.onAllNodesWithText("Ответы: 9").assertCountEquals(2)
        compose.onNodeWithText("Досок: 1").assertIsDisplayed()
        compose.onNodeWithText("Правил со срабатываниями: 1").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("dvach /b/").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Срабатываний: 12").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("ОП-посты: 3; ответы: 9").assertDoesNotExist()
        compose.onNodeWithText("Сбросить").performClick()
        compose.onNodeWithText("Сбросить статистику?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, repository.resets) }
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertEquals(0, repository.resets) }
        compose.onNodeWithText("Сбросить").performClick()
        compose.onNodeWithText("Сбросить счётчики").performClick()
        compose.runOnIdle { assertEquals(1, repository.resets) }
    }

    private fun showSettings(): StatisticsRepository {
        val rule = newContentFilterRule(defaultBoardAddress(), "голова")
        val repository =
            StatisticsRepository(
                ContentFilterStatistics(
                    mapOf(rule.id to 12L),
                    listOf(ContentFilterBoardStatistics("dvach", "b", 3L, 9L, 1700000000000L)),
                ),
            )
        val state = SettingsUiState(app = SettingsUiState().app.withContentFilters(listOf(rule)))
        compose.setContent {
            MaterialTheme {
                ContentFilterSettings(state, SettingsActions({}, {}, {}, filterStatistics = repository))
            }
        }
        return repository
    }
}

private class StatisticsRepository(
    snapshot: ContentFilterStatistics,
) : ContentFilterStatisticsRepository {
    override val statistics = MutableStateFlow(snapshot)
    var resets = 0
        private set

    override suspend fun record(hits: List<ContentFilterHit>): DomainOutcome<Unit> = DomainOutcome.Success(Unit)

    override suspend fun reset(): DomainOutcome<Unit> {
        resets += 1
        statistics.value = ContentFilterStatistics()
        return DomainOutcome.Success(Unit)
    }
}
