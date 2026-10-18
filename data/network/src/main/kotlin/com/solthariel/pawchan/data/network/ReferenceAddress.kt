package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId

internal fun referenceThread(href: String): ThreadAddress? {
    val match = REFERENCE_PATH.find(href)
    val board = match?.groupValues?.get(1)?.let(BoardId::parse)
    val thread =
        match
            ?.groupValues
            ?.get(2)
            ?.toLongOrNull()
            ?.let(ThreadId::parse)
    return if (board != null && thread != null) {
        ThreadAddress(BoardAddress(requireNotNull(SiteId.parse("dvach")), board), thread)
    } else {
        null
    }
}

private val REFERENCE_PATH =
    Regex("(?:^|https://(?:2ch\\.su|2ch\\.hk|2ch\\.pm))/([a-zA-Z0-9_]+)/(?:(?:arch/)?res)/(\\d+)\\.html(?:#.*)?$")
