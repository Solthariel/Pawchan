package com.solthariel.pawchan.domain.settings

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.flow.Flow

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
    TOMORROW,
    NORMIE,
    BURICHAN,
    HARDON,
    NEUTRON,
    PHOTON,
}

interface SettingsRepository {
    val theme: Flow<ThemePreference>
    val homeBoard: Flow<BoardAddress?>

    suspend fun setHomeBoard(board: BoardAddress): DomainOutcome<Unit>

    suspend fun setTheme(theme: ThemePreference): DomainOutcome<Unit>
}
