package com.solthariel.pawchan.domain.boards

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.flow.Flow

data class BoardDirectoryEntry(
    val address: BoardAddress,
    val title: String,
    val category: String,
    val postsPerHour: Int,
    val postingEnabled: Boolean,
    val requiresPostingAccess: Boolean = false,
)

interface BoardDirectoryRepository {
    suspend fun loadBoards(forceRefresh: Boolean = false): DomainOutcome<List<BoardDirectoryEntry>>

    val boardSpeeds: Flow<Map<BoardAddress, Int>> get() = kotlinx.coroutines.flow.flowOf(emptyMap())

    /** Publish an observed server value without requesting all boards again. */
    fun rememberBoardSpeed(
        board: BoardAddress,
        speed: Int,
    ) = Unit

    /** Synchronous memory-only fallback; never a network request. */
    fun knownBoardSpeed(board: BoardAddress): Int? = null
}

interface PostingAccessRepository {
    val hasPostingAccess: Flow<Boolean>
}

interface BoardPreferencesRepository {
    val favoriteBoards: Flow<Set<BoardId>>
    val expandedBoardCategories: Flow<Set<String>>

    suspend fun setFavoriteBoard(
        board: BoardId,
        favorite: Boolean,
    ): DomainOutcome<Unit>

    suspend fun setBoardCategoryExpanded(
        category: String,
        expanded: Boolean,
    ): DomainOutcome<Unit>
}

const val FAVORITES_BOARD_CATEGORY_KEY = "__favorites__"
