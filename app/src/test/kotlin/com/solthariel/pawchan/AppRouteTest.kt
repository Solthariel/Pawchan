package com.solthariel.pawchan

import com.solthariel.pawchan.navigation.AppRoute
import com.solthariel.pawchan.navigation.boardAddress
import com.solthariel.pawchan.navigation.saveableKey
import com.solthariel.pawchan.navigation.threadAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test
    fun threadRoutePreservesSiteBoardAndThread() {
        val address = requireNotNull(threadAddress("dvach", "po", 42L))

        assertEquals("thread/dvach/po/42", AppRoute.thread(address))
        assertEquals("dvach/po/42", address.saveableKey)
        assertEquals(
            "pawchan://thread/dvach/po/42",
            AppRoute.threadDeepLink(address),
        )
        assertEquals(address, AppRoute.threadAddress(AppRoute.thread(address)))
        assertEquals(null, AppRoute.threadAddress("catalog/dvach/po"))
    }

    @Test
    fun newThreadRoutePreservesBoard() {
        val board =
            boardAddress(
                requireNotNull(
                    com.solthariel.pawchan.domain.model.BoardId
                        .parse("po"),
                ),
            )

        assertEquals("new-thread/dvach/po", AppRoute.newThread(board))
    }

    @Test
    fun catalogRoutePreservesSiteAndBoard() {
        val board =
            requireNotNull(
                boardAddress(
                    site = "dvach",
                    board = "po",
                ),
            )

        assertEquals("catalog/dvach/po", AppRoute.catalog(board))
        assertEquals("dvach/po", board.saveableKey)
    }
}
