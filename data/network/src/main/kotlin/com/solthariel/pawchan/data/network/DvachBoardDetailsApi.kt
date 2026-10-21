package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request

class DvachBoardDetailsApi private constructor(
    private val callFactory: Call.Factory,
    private val decoder: BoardDetailsPayloadDecoder,
) : BoardDetailsRepository {
    override suspend fun loadArchive(
        board: BoardAddress,
        page: Int?,
    ): DomainOutcome<BoardArchiveSnapshot> {
        if (!board.isDvachSource()) return unsupportedDvachSource()
        require(page == null || page >= 0)
        val file = if (page == null) "index.json" else "$page.json"
        return executeDomainRequest(callFactory, boardDetailsRequest(board, listOf("arch", file))) {
            decoder.archive(board, it)
        }
    }

    override suspend fun loadInformation(board: BoardAddress): DomainOutcome<BoardInformation> =
        if (!board.isDvachSource()) {
            unsupportedDvachSource()
        } else {
            executeDomainRequest(callFactory, boardDetailsRequest(board, listOf("index.json"))) {
                decoder.information(board, it)
            }
        }

    companion object {
        fun create(callFactory: Call.Factory): DvachBoardDetailsApi =
            DvachBoardDetailsApi(callFactory, BoardDetailsPayloadDecoder(Json { ignoreUnknownKeys = true }))
    }
}

internal fun boardDetailsRequest(
    board: BoardAddress,
    segments: List<String>,
): Request {
    val url =
        HttpUrl
            .Builder()
            .scheme("https")
            .host(DVACH_HOST)
            .addPathSegment(board.board.value)
    segments.forEach(url::addPathSegment)
    return Request
        .Builder()
        .url(url.build())
        .header("Accept", JSON_MEDIA_TYPE)
        .header("User-Agent", USER_AGENT)
        .get()
        .build()
}
