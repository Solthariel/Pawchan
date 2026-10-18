package com.solthariel.pawchan

import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.catalog.BoardInformationState
import com.solthariel.pawchan.catalog.CatalogMenuViewModel
import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.navigation.boardAddress
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogMenuViewModelTest {
    @Test
    fun favoriteToggleIsSharedWithBoardDirectory() =
        withModel { model, repos ->
            model.toggleFavorite()
            advanceUntilIdle()
            assertTrue(model.preferences.value.favorite)
            assertEquals(
                setOf(
                    boardAddress(
                        requireNotNull(
                            com.solthariel.pawchan.domain.model.BoardId
                                .parse("biz"),
                        ),
                    ).board,
                ),
                repos.favoriteBoards.value,
            )
            model.toggleFavorite()
            advanceUntilIdle()
            assertFalse(model.preferences.value.favorite)
        }

    @Test
    fun homeAndThemeArePersistedThroughExistingRepositories() =
        withModel { model, repos ->
            model.makeHome()
            model.setTheme(ThemePreference.AMOLED)
            advanceUntilIdle()
            // Writes are serialized: a second click while saving is ignored.
            assertTrue(model.preferences.value.home)
            model.setTheme(ThemePreference.AMOLED)
            advanceUntilIdle()
            assertEquals(ThemePreference.AMOLED, repos.theme.value)
            assertEquals(
                "biz",
                repos.homeBoard.value
                    ?.board
                    ?.value,
            )
        }

    @Test
    fun storageFailureDoesNotPretendTheBoardWasSaved() =
        withModel { model, repos ->
            repos.failWrites = true
            model.toggleFavorite()
            advanceUntilIdle()
            assertFalse(model.preferences.value.favorite)
            assertTrue(model.preferences.value.saveFailed)
            model.dismiss()
            advanceUntilIdle()
            assertFalse(model.preferences.value.saveFailed)
        }

    @Test
    fun informationHasLoadingContentAndDismissalStates() =
        withModel { model, repos ->
            val board = requireNotNull(boardAddress("dvach", "biz"))
            repos.informationResult =
                DomainOutcome.Success(BoardInformation(board, "Бизнес", "Правила", null, null, null))
            repos.informationHold = CompletableDeferred()
            model.showInformation()
            runCurrent()
            assertEquals(BoardInformationState.Loading, model.information.value)
            repos.informationHold?.complete(Unit)
            advanceUntilIdle()
            assertTrue(model.information.value is BoardInformationState.Content)
            model.dismiss()
            assertEquals(BoardInformationState.Closed, model.information.value)
        }

    @Test
    fun dismissalCancelsLateInformationDelivery() =
        withModel { model, repos ->
            repos.informationHold = CompletableDeferred()
            model.showInformation()
            runCurrent()
            model.dismiss()
            repos.informationHold?.complete(Unit)
            advanceUntilIdle()
            assertEquals(BoardInformationState.Closed, model.information.value)
        }

    @Test
    fun actionsWaitForInitialPreferenceSnapshot() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repos = FakeBoardMenuRepositories()
            val model = CatalogMenuViewModel(requireNotNull(boardAddress("dvach", "biz")), repos, repos, repos)
            try {
                assertTrue(model.preferences.value.saving)
                model.toggleFavorite()
                advanceUntilIdle()
                assertFalse(model.preferences.value.favorite)
                model.toggleFavorite()
                advanceUntilIdle()
                assertTrue(model.preferences.value.favorite)
            } finally {
                model.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    private fun withModel(action: suspend TestScope.(CatalogMenuViewModel, FakeBoardMenuRepositories) -> Unit) =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val repos = FakeBoardMenuRepositories()
            val model = CatalogMenuViewModel(requireNotNull(boardAddress("dvach", "biz")), repos, repos, repos)
            try {
                advanceUntilIdle()
                action(model, repos)
            } finally {
                model.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}
