package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.CatalogSnapshot
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.serialization.json.Json
import okhttp3.Call

class DvachCatalogApi private constructor(
    private val callFactory: Call.Factory,
    private val decoder: CatalogPayloadDecoder,
    private val directory: BoardDirectoryRepository,
) : CatalogRepository {
    override suspend fun loadCatalog(board: BoardAddress): DomainOutcome<CatalogSnapshot> {
        if (!board.isDvachSource()) return unsupportedDvachSource()
        val catalog =
            executeDomainRequest(
                callFactory,
                CatalogRequestFactory.create(board.board),
                decoder::decode,
            )
        return if (catalog is DomainOutcome.Success && catalog.value.boardSpeed <= 0) {
            catalog.withFallbackSpeed(loadBoardPageSpeed(board))
        } else {
            catalog
        }
    }

    private suspend fun loadBoardPageSpeed(board: BoardAddress): Int {
        val boards = directory.loadBoards(forceRefresh = false)
        return (boards as? DomainOutcome.Success)
            ?.value
            ?.firstOrNull { entry -> entry.address == board }
            ?.postsPerHour
            ?: 0
    }

    companion object {
        fun create(
            callFactory: Call.Factory,
            directory: BoardDirectoryRepository,
        ): DvachCatalogApi {
            val json =
                dvachReadJson()
            return DvachCatalogApi(
                callFactory = callFactory,
                decoder = CatalogPayloadDecoder(json),
                directory = directory,
            )
        }
    }
}

private fun DomainOutcome.Success<CatalogSnapshot>.withFallbackSpeed(boardSpeed: Int): DomainOutcome<CatalogSnapshot> =
    if (boardSpeed > 0) {
        DomainOutcome.Success(
            CatalogSnapshot(
                board = value.board,
                threads = value.threads,
                boardSpeed = boardSpeed,
            ),
        )
    } else {
        this
    }
