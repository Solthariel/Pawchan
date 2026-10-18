package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.settings.ThemePreference

internal fun parseTheme(raw: String?): ThemePreference =
    ThemePreference.entries
        .firstOrNull { it.name == raw }
        ?: ThemePreference.SYSTEM

internal fun parseFavoriteBoards(raw: Set<String>): Set<BoardId> = raw.mapNotNullTo(linkedSetOf(), BoardId::parse)

internal fun parseHomeBoard(raw: String?): BoardAddress? {
    val parts = raw?.split('/').orEmpty()
    val site = parts.getOrNull(0)?.let(SiteId::parse)
    val board = parts.getOrNull(1)?.let(BoardId::parse)
    return if (parts.size == 2 && site != null && board != null) BoardAddress(site, board) else null
}
