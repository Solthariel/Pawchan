package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.model.BoardAddress
import java.util.UUID

internal fun newContentFilterRule(
    board: BoardAddress,
    text: String = "",
): ContentFilterRule =
    ContentFilterRule(
        id = UUID.randomUUID().toString(),
        site = board.site.value,
        boards = setOf(board.board.value),
        targets = ContentFilterTargets(threads = true, comments = true),
        condition = ContentFilterConditionSpec(ContentFilterMode.PHRASE, listOf(text)),
    )
