package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Request

class DvachBoardDirectoryApi private constructor(
    private val callFactory: Call.Factory,
    private val decoder: BoardDirectoryPayloadDecoder,
) : BoardDirectoryRepository {
    private val cacheMutex = Mutex()
    private var cachedBoards: List<BoardDirectoryEntry>? = null
    private val speeds = kotlinx.coroutines.flow.MutableStateFlow(BoardSpeeds())
    override val boardSpeeds =
        speeds.map { state -> state.directory + state.index.mapValues { it.value.speed } }.distinctUntilChanged()

    override fun rememberBoardSpeed(
        board: BoardAddress,
        speed: Int,
    ) {
        if (speed >= 0) {
            speeds.update { state ->
                val observation = IndexSpeed(speed, (state.index[board]?.revision ?: 0L) + 1L)
                state.copy(index = state.index + (board to observation))
            }
        }
    }

    override fun knownBoardSpeed(board: BoardAddress): Int? {
        val current = speeds.value
        return current.index[board]?.speed ?: current.directory[board]
    }

    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> =
        cacheMutex.withLock {
            if (!forceRefresh) {
                cachedBoards?.let { boards -> return@withLock DomainOutcome.Success(boards) }
            }
            val observedBeforeRequest = speeds.value.index
            executeDomainRequest(callFactory, BoardDirectoryRequestFactory.create(), decoder::decode)
                .also { outcome ->
                    if (outcome is DomainOutcome.Success) {
                        cachedBoards = outcome.value
                        val latest = outcome.value.associate { it.address to it.postsPerHour }
                        speeds.update { state ->
                            // An explicit fresh directory replaces older observations, never a concurrent index result.
                            val concurrent =
                                state.index.filter { (board, value) ->
                                    observedBeforeRequest[board] != value
                                }
                            state.copy(directory = latest, index = concurrent)
                        }
                    }
                }
        }

    companion object {
        fun create(callFactory: Call.Factory): DvachBoardDirectoryApi =
            DvachBoardDirectoryApi(
                callFactory = callFactory,
                decoder =
                    BoardDirectoryPayloadDecoder(
                        dvachReadJson(),
                    ),
            )
    }
}

private data class BoardSpeeds(
    val directory: Map<BoardAddress, Int> = emptyMap(),
    val index: Map<BoardAddress, IndexSpeed> = emptyMap(),
)

private data class IndexSpeed(
    val speed: Int,
    val revision: Long,
)

internal class BoardDirectoryPayloadDecoder(
    private val json: Json,
) {
    fun decode(payload: String): List<BoardDirectoryEntry>? =
        json
            .decodeFromString<BoardIndexDto>(payload)
            .boards
            .mapNotNull(BoardDirectoryDto::toDomain)
            .takeIf { boards -> boards.isNotEmpty() }
}

@Serializable
private data class BoardIndexDto(
    val boards: List<BoardDirectoryDto>,
)

@Serializable
private data class BoardDirectoryDto(
    val id: String,
    val name: String,
    val category: String = "",
    val speed: Int = 0,
    @SerialName("enable_posting")
    val enablePosting: Boolean = true,
)

private fun BoardDirectoryDto.toDomain(): BoardDirectoryEntry? {
    val board = BoardId.parse(id)
    return if (board != null && name.isNotBlank()) {
        BoardDirectoryEntry(
            address = BoardAddress(site = DVACH_SITE, board = board),
            title = name,
            category = category.trim(),
            postsPerHour = speed.coerceAtLeast(0),
            postingEnabled = enablePosting,
            requiresPostingAccess = board in POSTING_ACCESS_BOARDS,
        )
    } else {
        null
    }
}

internal object BoardDirectoryRequestFactory {
    fun create(): Request =
        Request
            .Builder()
            .url(
                HttpUrl
                    .Builder()
                    .scheme("https")
                    .host(DVACH_HOST)
                    .addPathSegment("index.json")
                    .build(),
            ).header("Accept", JSON_MEDIA_TYPE)
            .header("User-Agent", USER_AGENT)
            .get()
            .build()
}

private val DVACH_SITE = requireNotNull(SiteId.parse("dvach"))
private val POSTING_ACCESS_BOARDS =
    setOf("e", "nf", "fet", "ya", "r34", "ho", "h", "ga", "fur", "fg", "hg", "vn")
        .mapNotNullTo(mutableSetOf(), BoardId::parse)
