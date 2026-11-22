package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine

/** Full addresses are retained in both the merged directory and its speed cache. */
internal class SiteBoardDirectory(
    sources: List<ForumBoardSource>,
) : BoardDirectoryRepository {
    val sources = sources.toList()
    private val sites = this.sources.associate { it.site to it.directory }

    init {
        require(this.sources.isNotEmpty() && sites.size == this.sources.size)
    }

    override val boardSpeeds =
        combine(this.sources.map { it.directory.boardSpeeds }) { values ->
            values
                .flatMapIndexed { index, speeds -> speeds.filterKeys { it.site == this.sources[index].site }.entries }
                .associate { it.key to it.value }
        }

    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> =
        coroutineScope {
            val results = sources.map { source -> async { source.directory.loadBoards(forceRefresh) } }.awaitAll()
            val failure = results.filterIsInstance<DomainOutcome.Failure>().firstOrNull()
            val boards =
                results.filterIsInstance<DomainOutcome.Success<List<BoardDirectoryEntry>>>().flatMap { it.value }
            failure ?: DomainOutcome.Success(boards)
        }

    override fun rememberBoardSpeed(
        board: BoardAddress,
        speed: Int,
    ) {
        sites[board.site]?.rememberBoardSpeed(board, speed)
    }

    override fun knownBoardSpeed(board: BoardAddress): Int? = sites[board.site]?.knownBoardSpeed(board)
}
