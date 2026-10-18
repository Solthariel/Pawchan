package com.solthariel.pawchan.data.database

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId

internal fun storedThreadAddress(
    site: String,
    board: String,
    threadId: Long,
): ThreadAddress? {
    val parsedSite = SiteId.parse(site)
    val parsedBoard = BoardId.parse(board)
    val parsedThread = ThreadId.parse(threadId)
    return if (parsedSite != null && parsedBoard != null && parsedThread != null) {
        ThreadAddress(BoardAddress(parsedSite, parsedBoard), parsedThread)
    } else {
        null
    }
}
