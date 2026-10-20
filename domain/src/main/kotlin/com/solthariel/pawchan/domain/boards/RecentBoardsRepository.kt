package com.solthariel.pawchan.domain.boards

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome

/** Records a catalog visit and returns most-recent-first, distinct board addresses. */
interface RecentBoardsRepository {
    suspend fun visit(board: BoardAddress): DomainOutcome<List<BoardAddress>>
}
