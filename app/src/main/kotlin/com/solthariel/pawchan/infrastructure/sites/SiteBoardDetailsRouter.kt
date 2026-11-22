package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId

internal class SiteBoardDetailsRouter(
    repositories: Map<SiteId, BoardDetailsRepository>,
) : BoardDetailsRepository {
    private val sites = repositories.toMap()

    override suspend fun loadArchive(
        board: BoardAddress,
        page: Int?,
    ): DomainOutcome<BoardArchiveSnapshot> =
        sites[board.site]?.loadArchive(board, page) ?: DomainOutcome.Failure(DomainFailure.UnsupportedSite)

    override suspend fun loadInformation(board: BoardAddress): DomainOutcome<BoardInformation> =
        sites[board.site]?.loadInformation(board) ?: DomainOutcome.Failure(DomainFailure.UnsupportedSite)
}
