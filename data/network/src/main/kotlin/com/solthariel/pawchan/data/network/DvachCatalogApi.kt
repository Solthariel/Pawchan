package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.DomainOutcome
import okhttp3.Call

class DvachCatalogApi private constructor(
    private val callFactory: Call.Factory,
    private val decoder: CatalogPayloadDecoder,
    private val directory: BoardDirectoryRepository,
) : CatalogRepository {
    override suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot> {
        if (!board.isDvachSource()) return unsupportedDvachSource()
        val result = executeDomainRequest(callFactory, CatalogRequestFactory.create(board.board), decoder::decode)
        val known = directory.knownBoardSpeed(board)
        return if (result is DomainOutcome.Success && !result.value.boardSpeedAvailable && known != null) {
            DomainOutcome.Success(CatalogSnapshot(result.value.board, result.value.threads, known, true))
        } else {
            result
        }
    }

    override suspend fun loadBoardSpeed(board: BoardAddress): Int? {
        if (!board.isDvachSource()) return null
        val outcome = executeDomainRequest(callFactory, BoardSpeedRequest.create(board.board), ::decodeBoardSpeed)
        return (outcome as? DomainOutcome.Success)?.value?.also { directory.rememberBoardSpeed(board, it) }
    }

    companion object {
        fun create(
            callFactory: Call.Factory,
            directory: BoardDirectoryRepository,
        ): DvachCatalogApi = DvachCatalogApi(callFactory, CatalogPayloadDecoder(dvachReadJson()), directory)
    }
}
