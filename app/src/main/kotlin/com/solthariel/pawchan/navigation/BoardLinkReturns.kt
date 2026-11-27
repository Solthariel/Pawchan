package com.solthariel.pawchan.navigation

import androidx.navigation.NavController
import com.solthariel.pawchan.domain.model.BoardAddress

/**
 * Доска по ссылке из поста открывается как обычный выбор доски, но тред со ссылкой остаётся возвратом:
 * «Назад» с корня новой доски открывает его снова. Возвраты лежат в основании стека, поэтому выбор доски
 * вручную (новое основание) их сбрасывает, а переход по ссылке переносит дальше.
 */
internal fun NavController.openLinkedBoard(board: BoardAddress) {
    val from = currentBackStackEntry?.takeIf { it.destination.route == AppRoute.THREAD }?.readingRoute()
    val returns = boardReturns()
    navigateBoard(board)
    val base = entryOrNull(AppRoute.CATALOG) ?: return
    if (from != null && base.arguments?.toBoardAddress() == board && previousBackStackEntry == null) {
        base.savedStateHandle[BOARD_RETURNS] = ArrayList((returns + from).takeLast(MAX_BOARD_RETURNS))
    }
}

/** «Назад» с корня доски, открытой по ссылке: возвращает к треду со ссылкой. */
internal fun NavController.returnFromLinkedBoard(): Boolean {
    val returns = boardReturns()
    val address = returns.lastOrNull()?.let(AppRoute::threadAddress)
    if (address == null || !canReturnFromLinkedBoard()) return false
    navigateBoard(address.board)
    val rest = returns.dropLast(1)
    if (rest.isNotEmpty()) entryOrNull(AppRoute.CATALOG)?.savedStateHandle?.set(BOARD_RETURNS, ArrayList(rest))
    navigateThread(address)
    return true
}

internal fun NavController.canReturnFromLinkedBoard(): Boolean =
    previousBackStackEntry == null &&
        currentBackStackEntry?.destination?.route == AppRoute.CATALOG &&
        boardReturns().isNotEmpty()

private fun NavController.boardReturns(): List<String> =
    entryOrNull(AppRoute.CATALOG)?.savedStateHandle?.get<ArrayList<String>>(BOARD_RETURNS).orEmpty()

private const val BOARD_RETURNS = "boardReturns"
private const val MAX_BOARD_RETURNS = 10
