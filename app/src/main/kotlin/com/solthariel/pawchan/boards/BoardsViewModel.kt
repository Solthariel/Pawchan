package com.solthariel.pawchan.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.catalog.CatalogError
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.FAVORITES_BOARD_CATEGORY_KEY
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.isRateLimited
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface BoardsUiState {
    data object Loading : BoardsUiState

    data class Content(
        val boards: List<BoardDirectoryEntry>,
        val favoriteBoards: Set<BoardId>,
        val boardSpeeds: Map<BoardId, Int>,
        val unavailableBoards: Set<BoardId>,
        val expandedCategories: Set<String>,
        val isRefreshing: Boolean,
    ) : BoardsUiState

    data class Error(
        val reason: CatalogError,
        val isRefreshing: Boolean,
    ) : BoardsUiState
}

internal class BoardsViewModel(
    private val directory: BoardDirectoryRepository,
    private val preferences: BoardPreferencesRepository,
    private val postingAccess: PostingAccessRepository,
    processingDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val directoryOutcome = MutableStateFlow<DomainOutcome<List<BoardDirectoryEntry>>?>(null)
    private val refreshing = MutableStateFlow(false)
    private val transientExpandedCategories = MutableStateFlow(emptySet<String>())
    private val directoryState =
        combine(directoryOutcome, refreshing, directory.boardSpeeds) { outcome, isRefreshing, speeds ->
            val updated =
                if (outcome is DomainOutcome.Success) {
                    DomainOutcome.Success(
                        outcome.value.map { entry ->
                            speeds[entry.address]?.let { entry.copy(postsPerHour = it) } ?: entry
                        },
                    )
                } else {
                    outcome
                }
            BoardDirectoryState(updated, isRefreshing)
        }

    val state: StateFlow<BoardsUiState> =
        combine(
            directoryState,
            preferences.favoriteBoards,
            postingAccess.hasPostingAccess,
            preferences.expandedBoardCategories,
            transientExpandedCategories,
        ) { directory, favorites, hasPostingAccess, savedCategories, transientCategories ->
            when (val outcome = directory.outcome) {
                null -> {
                    BoardsUiState.Loading
                }

                is DomainOutcome.Success -> {
                    val unavailable =
                        outcome.value
                            .filter { board ->
                                board.requiresPostingAccess && !hasPostingAccess
                            }.mapTo(mutableSetOf()) { board -> board.address.board }
                    BoardsUiState.Content(
                        boards = outcome.value,
                        favoriteBoards = favorites,
                        boardSpeeds =
                            outcome.value.associate { board ->
                                board.address.board to board.postsPerHour
                            },
                        unavailableBoards = unavailable,
                        expandedCategories =
                            savedCategories.filterTo(mutableSetOf()) { it == FAVORITES_BOARD_CATEGORY_KEY } +
                                transientCategories,
                        isRefreshing = directory.isRefreshing,
                    )
                }

                is DomainOutcome.Failure -> {
                    BoardsUiState.Error(
                        reason = outcome.reason.toCatalogError(),
                        isRefreshing = directory.isRefreshing,
                    )
                }
            }
        }.flowOn(processingDispatcher).stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = BoardsUiState.Loading,
        )

    init {
        refresh()
    }

    fun refresh() {
        if (refreshing.value) {
            return
        }
        viewModelScope.launch {
            refreshing.value = true
            try {
                directoryOutcome.value = directory.loadBoards(forceRefresh = directoryOutcome.value != null)
            } finally {
                refreshing.value = false
            }
        }
    }

    fun setFavorite(
        board: BoardId,
        favorite: Boolean,
    ) {
        viewModelScope.launch {
            preferences.setFavoriteBoard(board, favorite)
        }
    }

    fun setCategoryExpanded(
        category: String,
        expanded: Boolean,
    ) {
        if (category == FAVORITES_BOARD_CATEGORY_KEY) {
            viewModelScope.launch {
                preferences.setBoardCategoryExpanded(category, expanded)
            }
        } else {
            transientExpandedCategories.value =
                if (expanded) {
                    transientExpandedCategories.value + category
                } else {
                    transientExpandedCategories.value - category
                }
        }
    }

    companion object {
        fun factory(
            directory: BoardDirectoryRepository,
            preferences: BoardPreferencesRepository,
            postingAccess: PostingAccessRepository,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    BoardsViewModel(directory, preferences, postingAccess)
                }
            }
    }
}

private data class BoardDirectoryState(
    val outcome: DomainOutcome<List<BoardDirectoryEntry>>?,
    val isRefreshing: Boolean,
)

internal data class BoardCategoryUi(
    val title: String,
    val boards: List<BoardDirectoryEntry>,
)

internal fun boardCategories(boards: List<BoardDirectoryEntry>): List<BoardCategoryUi> {
    val grouped = boards.groupBy { board -> board.category.ifBlank { OTHER_CATEGORY } }
    return grouped
        .map { (title, entries) ->
            BoardCategoryUi(
                title = CATEGORY_LABELS[title] ?: title,
                boards = entries.sortedBy { it.address.board.value },
            )
        }.sortedWith(
            compareBy<BoardCategoryUi> { categoryRank(it.title) }
                .thenBy { it.title.lowercase() },
        )
}

private fun categoryRank(title: String): Int {
    val normalized = CATEGORY_LABELS.entries.firstOrNull { it.value == title }?.key ?: title
    return CATEGORY_ORDER.indexOf(normalized).takeIf { it >= 0 } ?: CATEGORY_ORDER.size
}

private val CATEGORY_ORDER =
    listOf(
        "Тематика",
        "Творчество",
        "Политика",
        "Техника и софт",
        "Игры",
        "Взрослым",
        "Японская культура",
        "Разное",
        OTHER_CATEGORY,
        USER_BOARDS_CATEGORY,
    )

private val CATEGORY_LABELS =
    mapOf(
        "Политика" to "Политика и новости",
        "Взрослым" to "Взрослым (18+)",
    )

internal fun filterBoards(
    boards: List<BoardDirectoryEntry>,
    query: String,
): List<BoardDirectoryEntry> {
    val normalized = query.trim()
    val normalizedCode = normalized.trim('/')
    return if (normalized.isEmpty()) {
        boards
    } else {
        boards.filter { board ->
            val codeMatches =
                board.address.board.value
                    .contains(normalizedCode, ignoreCase = true)
            val titleMatches = board.title.contains(normalized, ignoreCase = true)
            val categoryMatches = board.category.contains(normalized, ignoreCase = true)
            codeMatches || titleMatches || categoryMatches
        }
    }
}

private fun DomainFailure.toCatalogError(): CatalogError =
    when (this) {
        DomainFailure.NetworkUnavailable -> {
            CatalogError.NETWORK
        }

        DomainFailure.NetworkTimeout -> {
            CatalogError.TIMEOUT
        }

        DomainFailure.SecureConnectionFailed -> {
            CatalogError.SECURE_CONNECTION
        }

        DomainFailure.NotFound -> {
            CatalogError.NOT_FOUND
        }

        DomainFailure.StorageUnavailable -> {
            CatalogError.STORAGE
        }

        DomainFailure.UnsupportedSite, DomainFailure.InvalidResponse -> {
            CatalogError.INVALID_RESPONSE
        }

        is DomainFailure.HttpStatus -> {
            if (isRateLimited) CatalogError.RATE_LIMITED else CatalogError.SERVER
        }

        is DomainFailure.ProtectionChallenge -> {
            CatalogError.PROTECTION
        }
    }

private const val USER_BOARDS_CATEGORY = "Пользовательские"
private const val OTHER_CATEGORY = "Остальное"
