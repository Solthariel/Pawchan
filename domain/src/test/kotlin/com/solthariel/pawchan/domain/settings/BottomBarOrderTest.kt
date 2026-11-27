package com.solthariel.pawchan.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomBarOrderTest {
    @Test
    fun defaultOrderIsTheOneEveryoneHasToday() {
        assertEquals(
            listOf(
                BottomBarButton.CATALOG,
                BottomBarButton.THREADS,
                BottomBarButton.BOARDS,
                BottomBarButton.SETTINGS,
                BottomBarButton.WRITE,
            ),
            NavigationPreferences().normalized().bottomBarOrder,
        )
    }

    @Test
    fun storedOrderRoundTrips() {
        val order =
            listOf(
                BottomBarButton.SETTINGS,
                BottomBarButton.CATALOG,
                BottomBarButton.THREADS,
                BottomBarButton.WRITE,
                BottomBarButton.BOARDS,
            )

        assertEquals(order, parseBottomBarOrder(order.bottomBarOrderValue()))
    }

    @Test
    fun brokenValueKeepsEveryButtonExactlyOnce() {
        assertEquals(
            listOf(
                BottomBarButton.WRITE,
                BottomBarButton.CATALOG,
                BottomBarButton.THREADS,
                BottomBarButton.BOARDS,
                BottomBarButton.SETTINGS,
            ),
            parseBottomBarOrder("write, write,unknown,catalog"),
        )
    }

    @Test
    fun emptyValueFallsBackToTheDefaultOrder() {
        assertEquals(BottomBarButton.entries, parseBottomBarOrder(""))
    }

    @Test
    fun keyboardOpensByDefault() {
        assertEquals(true, NavigationPreferences().autoOpenKeyboard)
    }
}
