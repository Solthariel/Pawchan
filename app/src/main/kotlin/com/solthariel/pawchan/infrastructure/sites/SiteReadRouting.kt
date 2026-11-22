package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ReportingThreadRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository

/** Explicit, immutable routing; unsupported sites are never sent to another source. */
internal class SiteCatalogRouter(
    repositories: Map<SiteId, CatalogRepository>,
) : CatalogRepository {
    private val sites = repositories.toMap()

    override suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot> =
        sites[board.site]?.loadCatalog(board) ?: DomainOutcome.Failure(DomainFailure.UnsupportedSite)

    override suspend fun loadBoardSpeed(board: BoardAddress): Int? = sites[board.site]?.loadBoardSpeed(board)
}

internal class SiteThreadRouter(
    repositories: Map<SiteId, ThreadRepository>,
) : ReportingThreadRepository {
    private val sites = repositories.toMap()

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
        sites[address.board.site]?.loadThread(address) ?: DomainOutcome.Failure(DomainFailure.UnsupportedSite)

    override fun supportsReporting(address: ThreadAddress): Boolean =
        (sites[address.site] as? ReportingThreadRepository)?.supportsReporting(address) == true

    override suspend fun reportPost(
        address: ThreadAddress,
        post: com.solthariel.pawchan.domain.model.PostId,
        comment: String,
    ): com.solthariel.pawchan.domain.posting.PostingOutcome<Unit> =
        (sites[address.site] as? ReportingThreadRepository)
            ?.takeIf { it.supportsReporting(address) }
            ?.reportPost(address, post, comment)
            ?: com.solthariel.pawchan.domain.posting.PostingOutcome.Failure(
                com.solthariel.pawchan.domain.posting.PostingFailure
                    .Rejected("Жалобы не поддерживаются этим источником"),
            )

    override suspend fun isThreadDeleted(address: ThreadAddress): Boolean {
        val repository = sites[address.board.site]
        return repository?.isThreadDeleted(address) ?: false
    }
}
