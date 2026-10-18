package com.solthariel.pawchan.domain.boards

import com.solthariel.pawchan.domain.model.BoardId

/** Exact board codes work before the directory is loaded and for custom boards. */
fun resolveBoardSearch(
    query: String,
    boards: List<BoardDirectoryEntry>,
): BoardId? {
    val normalized = query.trim().trim('/').lowercase()
    val exact = boards.firstOrNull { it.address.board.value == normalized }?.address?.board
    val titles = boards.filter { it.title.equals(normalized, ignoreCase = true) }
    return when {
        normalized.isEmpty() -> null
        exact != null -> exact
        titles.size == 1 -> titles.single().address.board
        normalized.isBoardCode() -> BoardId.parse(normalized)
        else -> null
    }
}

private fun String.isBoardCode(): Boolean = all { it in 'a'..'z' || it in '0'..'9' || it == '_' || it == '-' }
