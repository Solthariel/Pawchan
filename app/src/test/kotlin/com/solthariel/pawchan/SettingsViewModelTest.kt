package com.solthariel.pawchan

import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.settings.SettingsRepository
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.navigation.boardAddress
import com.solthariel.pawchan.settings.SettingsViewModel
import com.solthariel.pawchan.settings.toPostDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun appliesAndPersistsSelectedTheme() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val repository = FakeSettingsRepository()
                val viewModel = SettingsViewModel(repository, FakeAppSettingsRepository())
                advanceUntilIdle()
                assertEquals(ThemePreference.SYSTEM, viewModel.theme.value)

                viewModel.setTheme(ThemePreference.AMOLED)
                advanceUntilIdle()

                assertEquals(ThemePreference.AMOLED, viewModel.theme.value)
                assertEquals(ThemePreference.AMOLED, repository.savedTheme)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun appSettingsChangesReachStateAndPostDisplay() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val model = SettingsViewModel(FakeSettingsRepository(), FakeAppSettingsRepository())
                advanceUntilIdle()
                model.updateApp { it.copy(postMarkers = false, textSize = 15) }
                advanceUntilIdle()
                val display =
                    model.state.value.app
                        .toPostDisplay()
                assertEquals(java.time.ZoneId.systemDefault(), display.zone)
                assertEquals(false, display.showStateMarkers)
                assertEquals(15, display.textSize)
                model.viewModelScope.cancel()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun persistedHomeIsLoadedBeforeChoosingInitialBoard() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = FakeSettingsRepository()
                val board = requireNotNull(boardAddress("dvach", "biz"))
                repository.homeBoard.value = board
                val model = SettingsViewModel(repository, FakeAppSettingsRepository())
                assertEquals(null, model.homeBoard.value)
                advanceUntilIdle()
                assertEquals(board, model.homeBoard.value?.board)
                model.viewModelScope.cancel()
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class FakeAppSettingsRepository : AppSettingsRepository {
    override val appSettings = MutableStateFlow(AppSettings())

    override suspend fun updateAppSettings(transform: (AppSettings) -> AppSettings): DomainOutcome<Unit> {
        appSettings.value = transform(appSettings.value)
        return DomainOutcome.Success(Unit)
    }
}

private class FakeSettingsRepository : SettingsRepository {
    override val homeBoard = MutableStateFlow<com.solthariel.pawchan.domain.model.BoardAddress?>(null)

    override suspend fun setHomeBoard(board: com.solthariel.pawchan.domain.model.BoardAddress): DomainOutcome<Unit> {
        homeBoard.value = board
        return DomainOutcome.Success(Unit)
    }

    private val mutableTheme = MutableStateFlow(ThemePreference.SYSTEM)
    override val theme: Flow<ThemePreference> = mutableTheme

    var savedTheme: ThemePreference = ThemePreference.SYSTEM
        private set

    override suspend fun setTheme(theme: ThemePreference): DomainOutcome<Unit> {
        savedTheme = theme
        mutableTheme.value = theme
        return DomainOutcome.Success(Unit)
    }
}
