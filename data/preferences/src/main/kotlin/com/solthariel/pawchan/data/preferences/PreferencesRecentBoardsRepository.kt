package com.solthariel.pawchan.data.preferences

import android.content.Context
import com.solthariel.pawchan.domain.boards.RecentBoardsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** App-owned, device-local catalog history. Disk access is confined to IO. */
class PreferencesRecentBoardsRepository(
    context: Context,
) : RecentBoardsRepository {
    private val application = context.applicationContext
    private val mutex = Mutex()

    override suspend fun visit(board: BoardAddress): DomainOutcome<List<BoardAddress>> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                try {
                    val storage = application.getSharedPreferences("recent-boards", Context.MODE_PRIVATE)
                    val recent = updateRecentBoards(storage.getString("visits", "").orEmpty(), board)
                    val encoded = recent.joinToString("\n") { "${it.site.value}/${it.board.value}" }
                    if (storage.edit().putString("visits", encoded).commit()) {
                        DomainOutcome.Success(recent)
                    } else {
                        DomainOutcome.Failure(DomainFailure.StorageUnavailable)
                    }
                } catch (_: SecurityException) {
                    DomainOutcome.Failure(DomainFailure.StorageUnavailable)
                }
            }
        }
}

internal fun updateRecentBoards(
    encoded: String,
    visited: BoardAddress,
): List<BoardAddress> =
    (sequenceOf(visited) + encoded.lineSequence().mapNotNull(::decodeRecentBoard))
        .distinct()
        .take(RECENT_BOARD_LIMIT)
        .toList()

private fun decodeRecentBoard(value: String): BoardAddress? {
    val segments = value.split('/')
    if (segments.size != 2) return null
    val site = SiteId.parse(segments[0])
    val board = BoardId.parse(segments[1])
    return if (site != null && board != null) BoardAddress(site, board) else null
}

private const val RECENT_BOARD_LIMIT = 100
