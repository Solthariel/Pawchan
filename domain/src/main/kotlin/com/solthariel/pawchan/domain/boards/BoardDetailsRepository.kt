package com.solthariel.pawchan.domain.boards

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress

data class ArchivedThreadSummary(
    val address: ThreadAddress,
    val subject: String,
)

data class BoardArchiveSnapshot(
    val threads: List<ArchivedThreadSummary>,
    val pages: List<Int>,
)

data class BoardInformation(
    val address: BoardAddress,
    val title: String,
    val description: String,
    val defaultName: String?,
    val bumpLimit: Int?,
    val maxCommentLength: Int?,
    val configuration: BoardConfiguration = BoardConfiguration(),
)

interface BoardDetailsRepository {
    suspend fun loadArchive(
        board: BoardAddress,
        page: Int? = null,
    ): DomainOutcome<BoardArchiveSnapshot>

    suspend fun loadInformation(board: BoardAddress): DomainOutcome<BoardInformation>
}
