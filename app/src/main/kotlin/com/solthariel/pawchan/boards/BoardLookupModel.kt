package com.solthariel.pawchan.boards

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId

internal data class BoardLookupBoard(
    val address: BoardAddress,
    val title: String,
    val selected: Boolean,
) {
    val key: String = "${address.site.value}/${address.board.value}"
    val code: String = "/${address.board.value}/"
    val description: String = "$code $title"
}

internal data class BoardLookupSource(
    val boards: List<BoardDirectoryEntry> = emptyList(),
    val favorites: Set<BoardId> = emptySet(),
    val recent: List<BoardAddress> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false,
)

internal data class BoardLookupContent(
    val query: String,
    val favorites: List<BoardLookupBoard>,
    val recent: List<BoardLookupBoard>,
    val suggestions: List<BoardLookupBoard>,
)

internal fun BoardLookupSource.prepareLookup(
    current: BoardAddress,
    query: String,
): BoardLookupContent {
    val available = boards.filter { it.address.site == current.site }.distinctBy { it.address }
    val byAddress = available.associateBy { it.address }

    fun render(address: BoardAddress) =
        BoardLookupBoard(
            address,
            byAddress[address]?.title.orEmpty(),
            address == current,
        )
    val favoriteAddresses = favorites.map { BoardAddress(current.site, it) }
    val recentAddresses = recent.filter { it.site == current.site && it !in favoriteAddresses }.distinct()
    val needle = query.trim().trim('/').lowercase()
    val suggestions =
        if (needle.isEmpty()) {
            emptyList()
        } else {
            available
                .mapNotNull { entry ->
                    lookupRank(entry, needle)?.let { rank -> rank to entry }
                }.sortedWith(compareBy({ it.first }, { it.second.address.board.value }))
                .map { render(it.second.address) }
        }
    return BoardLookupContent(
        query,
        favoriteAddresses.map(::render),
        recentAddresses.map(::render),
        suggestions,
    )
}

private fun lookupRank(
    entry: BoardDirectoryEntry,
    query: String,
): Int? {
    val code =
        entry.address.board.value
            .lowercase()
    val title = entry.title.lowercase()
    return when {
        code == query -> 0
        code.startsWith(query) -> 1
        title.startsWith(query) -> 2
        query in code || query in title -> LOOKUP_SUBSTRING_RANK
        else -> null
    }
}

internal data class BoardLookupRowBudget(
    val favorites: Int,
    val recent: Int,
)

internal fun boardLookupRowBudget(
    favoriteRows: Int,
    recentRows: Int,
): BoardLookupRowBudget = BoardLookupRowBudget(favoriteRows.coerceIn(0, 2), recentRows.coerceIn(0, 2))

internal const val BOARD_LOOKUP_MAX_ROWS = 4
private const val LOOKUP_SUBSTRING_RANK = 3
