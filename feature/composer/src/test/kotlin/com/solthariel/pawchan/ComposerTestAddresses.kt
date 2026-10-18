package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId

internal fun defaultBoardAddress(): BoardAddress =
    BoardAddress(
        requireNotNull(SiteId.parse("dvach")),
        requireNotNull(BoardId.parse("b")),
    )

internal fun defaultThreadAddress(id: Long): ThreadAddress =
    ThreadAddress(
        defaultBoardAddress(),
        requireNotNull(ThreadId.parse(id)),
    )
