package com.solthariel.pawchan.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import com.solthariel.pawchan.domain.settings.SettingsRepository
import com.solthariel.pawchan.domain.settings.ThemePreference
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

internal class SettingsViewModel(
    private val repository: SettingsRepository,
    private val appRepository: AppSettingsRepository,
    val filterStatistics: ContentFilterStatisticsRepository? = null,
) : ViewModel() {
    val homeBoard: StateFlow<HomeBoardSetting?> =
        repository.homeBoard.map { HomeBoardSetting(it) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val theme: StateFlow<ThemePreference> =
        repository.theme.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ThemePreference.SYSTEM,
        )

    val loaded: StateFlow<AppSettings?> =
        appRepository.appSettings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val app: StateFlow<AppSettings> =
        loaded.filterNotNull().stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val state: StateFlow<SettingsUiState> =
        combine(theme, app, homeBoard) { theme, app, home -> SettingsUiState(theme, app, home?.board) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    val writeError = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    fun setTheme(theme: ThemePreference) {
        viewModelScope.launch {
            writeError.value =
                if (repository.setTheme(theme) is DomainOutcome.Failure) {
                    "Не удалось сохранить тему"
                } else {
                    null
                }
        }
    }

    fun updateApp(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            writeError.value =
                if (appRepository.updateAppSettings(transform) is DomainOutcome.Failure) {
                    "Не удалось сохранить настройки"
                } else {
                    null
                }
        }
    }

    suspend fun restoreContentFilters(rules: List<ContentFilterRule>): Int? {
        var added = 0
        val outcome =
            appRepository.updateAppSettings { current ->
                val merged = mergeContentFilterBackup(current.contentFilters, rules)
                added = merged.size - current.contentFilters.size
                current.withContentFilters(merged)
            }
        return if (outcome is DomainOutcome.Success) added else null
    }

    companion object {
        fun factory(
            repository: SettingsRepository,
            appRepository: AppSettingsRepository,
            statistics: ContentFilterStatisticsRepository? = null,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SettingsViewModel(repository, appRepository, statistics)
                }
            }
    }
}

internal data class HomeBoardSetting(
    val board: BoardAddress?,
)

internal data class SettingsUiState(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val app: AppSettings = AppSettings(),
    val homeBoard: BoardAddress? = null,
)

internal data class SettingsActions(
    val onTheme: (ThemePreference) -> Unit,
    val onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    val onBack: () -> Unit,
    val onRestoreFilters: (suspend (List<ContentFilterRule>) -> Int?)? = null,
    val filterStatistics: ContentFilterStatisticsRepository? = null,
    val onStatistics: () -> Unit = {},
)

/** Настройки вида поста для [com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences]. */
internal fun AppSettings.toPostDisplay(): PostDisplayPreferences =
    PostDisplayPreferences(
        zone = ZoneId.systemDefault(),
        showStateMarkers = postMarkers,
        bubblePosts = bubblePosts,
        hideStickers = hideStickers,
        showDefaultName = showDefaultName,
        textSize = textSize,
        collapseLines = collapseLines,
    )
