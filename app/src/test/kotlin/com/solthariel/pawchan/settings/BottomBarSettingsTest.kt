package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.BottomBarButton
import com.solthariel.pawchan.navigation.chrome.bottomBarVisible
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarSettingsTest {
    @Test
    fun movingStopsAtTheEdges() {
        val list = listOf("a", "b", "c")
        assertEquals(listOf("b", "a", "c"), list.moved(1, -1))
        assertEquals(listOf("a", "c", "b"), list.moved(1, 1))
        assertEquals(list, list.moved(0, -1))
        assertEquals(list, list.moved(2, 1))
        assertEquals(list, list.moved(5, -1))
    }

    @Test
    fun savedOrderKeepsEveryButtonOnce() {
        val navigation =
            AppSettings()
                .withBottomBarOrder(listOf(BottomBarButton.WRITE))
                .behavior.client.reading.navigation
        assertEquals(
            listOf(
                BottomBarButton.WRITE,
                BottomBarButton.CATALOG,
                BottomBarButton.THREADS,
                BottomBarButton.BOARDS,
                BottomBarButton.SETTINGS,
            ),
            navigation.bottomBarOrder,
        )
    }

    @Test
    fun keyboardSettingIsOnByDefaultAndCanBeTurnedOff() {
        assertTrue(
            AppSettings()
                .behavior.client.reading.navigation.autoOpenKeyboard,
        )
        assertFalse(
            AppSettings()
                .withAutoOpenKeyboard(false)
                .behavior.client.reading.navigation.autoOpenKeyboard,
        )
    }

    @Test
    fun barIsHiddenWhileTheKeyboardIsShown() {
        assertFalse(bottomBarVisible(keyboardShown = true))
        assertTrue(bottomBarVisible(keyboardShown = false))
    }
}
