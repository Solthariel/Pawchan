package com.solthariel.pawchan.navigation

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId

internal fun defaultThreadAddress(threadId: Long): ThreadAddress =
    ThreadAddress(
        board = defaultBoardAddress(),
        thread = requireNotNull(ThreadId.parse(threadId)),
    )

internal fun defaultBoardAddress(): BoardAddress = boardAddress(requireNotNull(BoardId.parse(DEFAULT_BOARD)))

internal fun boardAddress(board: BoardId): BoardAddress =
    BoardAddress(
        site = requireNotNull(SiteId.parse(DEFAULT_SITE)),
        board = board,
    )

internal fun boardAddress(
    site: String,
    board: String,
): BoardAddress? {
    val siteId = SiteId.parse(site)
    val boardId = BoardId.parse(board)
    return if (siteId != null && boardId != null) {
        BoardAddress(site = siteId, board = boardId)
    } else {
        null
    }
}

internal fun threadAddress(
    site: String,
    board: String,
    threadId: Long,
): ThreadAddress? {
    val parsedBoard = boardAddress(site, board)
    val parsedThreadId = ThreadId.parse(threadId)
    return if (parsedBoard != null && parsedThreadId != null) {
        ThreadAddress(
            board = parsedBoard,
            thread = parsedThreadId,
        )
    } else {
        null
    }
}

internal val BoardAddress.saveableKey: String
    get() = "${site.value}/${board.value}"

internal val ThreadAddress.saveableKey: String
    get() = "${site.value}/${board.board.value}/${thread.value}"

internal const val DEFAULT_BOARD = "b"
private const val DEFAULT_SITE = "dvach"
