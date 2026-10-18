package com.solthariel.pawchan

import com.solthariel.pawchan.boards.FAVORITES_CATEGORY_KEY
import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.SettingsRepository
import com.solthariel.pawchan.domain.settings.ThemePreference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeBoardMenuRepositories :
    SettingsRepository,
    BoardPreferencesRepository,
    BoardDetailsRepository {
    override val theme = MutableStateFlow(ThemePreference.SYSTEM)
    override val homeBoard = MutableStateFlow<BoardAddress?>(null)
    override val favoriteBoards = MutableStateFlow(emptySet<BoardId>())
    override val expandedBoardCategories = MutableStateFlow(setOf(FAVORITES_CATEGORY_KEY))
    var failWrites = false
    var informationHold: CompletableDeferred<Unit>? = null
    var informationResult: DomainOutcome<BoardInformation> = DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
    var archiveResult: DomainOutcome<BoardArchiveSnapshot> =
        DomainOutcome.Success(BoardArchiveSnapshot(emptyList(), listOf(0, 1, 2)))
    val archiveRequests = mutableListOf<Int?>()

    override suspend fun setTheme(theme: ThemePreference): DomainOutcome<Unit> = write { this.theme.value = theme }

    override suspend fun setHomeBoard(board: BoardAddress): DomainOutcome<Unit> = write { homeBoard.value = board }

    override suspend fun setFavoriteBoard(
        board: BoardId,
        favorite: Boolean,
    ): DomainOutcome<Unit> =
        write {
            favoriteBoards.value = if (favorite) favoriteBoards.value + board else favoriteBoards.value - board
        }

    override suspend fun setBoardCategoryExpanded(
        category: String,
        expanded: Boolean,
    ): DomainOutcome<Unit> =
        write {
            expandedBoardCategories.value =
                if (expanded) {
                    expandedBoardCategories.value + category
                } else {
                    expandedBoardCategories.value - category
                }
        }

    private fun write(action: () -> Unit): DomainOutcome<Unit> =
        if (failWrites) {
            DomainOutcome.Failure(DomainFailure.StorageUnavailable)
        } else {
            action()
            DomainOutcome.Success(Unit)
        }

    override suspend fun loadInformation(board: BoardAddress): DomainOutcome<BoardInformation> {
        informationHold?.await()
        return informationResult
    }

    override suspend fun loadArchive(
        board: BoardAddress,
        page: Int?,
    ): DomainOutcome<BoardArchiveSnapshot> {
        archiveRequests += page
        return archiveResult
    }
}
