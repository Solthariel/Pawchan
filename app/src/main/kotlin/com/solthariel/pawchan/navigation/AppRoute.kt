package com.solthariel.pawchan.navigation

import android.net.Uri
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId

internal object AppRoute {
    const val LIBRARY = "library"
    const val HISTORY = "history"
    const val COPIES = "copies"
    const val SETTINGS = "settings"
    const val STATISTICS = "statistics"
    const val BOARDS = "boards"
    const val SITE_ARGUMENT = "site"
    const val BOARD_ARGUMENT = "board"
    const val THREAD_ARGUMENT = "threadId"
    const val CATALOG = "catalog/{$SITE_ARGUMENT}/{$BOARD_ARGUMENT}"
    const val ARCHIVE = "archive/{$SITE_ARGUMENT}/{$BOARD_ARGUMENT}"
    const val NEW_THREAD = "new-thread/{$SITE_ARGUMENT}/{$BOARD_ARGUMENT}"
    const val THREAD = "thread/{$SITE_ARGUMENT}/{$BOARD_ARGUMENT}/{$THREAD_ARGUMENT}"
    const val THREAD_DEEP_LINK = "pawchan://thread/{$SITE_ARGUMENT}/{$BOARD_ARGUMENT}/{$THREAD_ARGUMENT}"
    const val CHALLENGE_ARGUMENT = "url"
    const val CHALLENGE_RETRY_TOKEN = "challengeRetryToken"
    const val CHALLENGE = "challenge?$CHALLENGE_ARGUMENT={$CHALLENGE_ARGUMENT}"

    fun thread(address: ThreadAddress): String {
        val site = address.site.value
        val board = address.board.board.value
        return "thread/$site/$board/${address.thread.value}"
    }

    fun threadAddress(route: String): ThreadAddress? {
        val parts = route.split('/')
        val valid = parts.size == THREAD_ROUTE_PARTS && parts[0] == "thread"
        val board = if (valid) catalogBoard("catalog/${parts[1]}/${parts[2]}") else null
        val thread = parts.getOrNull(THREAD_ID_PART)?.toLongOrNull()?.let(ThreadId::parse)
        return if (board != null && thread != null) ThreadAddress(board, thread) else null
    }

    fun threadDeepLink(address: ThreadAddress): String =
        "pawchan://thread/${address.site.value}/${address.board.board.value}/${address.thread.value}"

    fun catalog(board: BoardAddress): String = "catalog/${board.site.value}/${board.board.value}"

    fun catalogBoard(route: String): BoardAddress? {
        val parts = route.split('/')
        val catalog = parts.size == CATALOG_ROUTE_PARTS && parts[0] == "catalog"
        val site = parts.getOrNull(1)?.let(SiteId::parse)
        val board = parts.getOrNull(2)?.let(BoardId::parse)
        return if (catalog && site != null && board != null) BoardAddress(site, board) else null
    }

    fun archive(board: BoardAddress): String = "archive/${board.site.value}/${board.board.value}"

    fun newThread(board: BoardAddress): String = "new-thread/${board.site.value}/${board.board.value}"

    fun challenge(url: String): String = "challenge?$CHALLENGE_ARGUMENT=${Uri.encode(url)}"
}

private const val CATALOG_ROUTE_PARTS = 3
private const val THREAD_ROUTE_PARTS = 4
private const val THREAD_ID_PART = 3
