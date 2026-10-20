package com.solthariel.pawchan.boards

import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.settings.SettingsRepository

internal data class BoardRepositories(
    val directory: BoardDirectoryRepository,
    val details: BoardDetailsRepository,
    val settings: SettingsRepository,
    val preferences: BoardPreferencesRepository,
    val postingAccess: PostingAccessRepository,
    val recent: com.solthariel.pawchan.domain.boards.RecentBoardsRepository? = null,
)
