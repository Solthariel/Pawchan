package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.solthariel.pawchan.domain.settings.BottomBarButton
import com.solthariel.pawchan.domain.settings.NavigationLayout
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.domain.settings.ThreadsHubItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Новые настройки 0.1.53: порядок кнопок, разделы «Тредов» и клавиатура переживают сохранение. */
class NavigationLayoutStoreTest {
    @Test
    fun storeWithoutNewKeysKeepsTheOldLayoutAndOpensTheKeyboard() {
        val navigation = mutablePreferencesOf().readNavigation()
        assertEquals(BottomBarButton.entries, navigation.bottomBarOrder)
        assertEquals(ThreadsHubItem.entries, navigation.layout.threads)
        assertEquals(emptySet<ThreadsHubItem>(), navigation.layout.threadsInMenu)
        assertTrue(navigation.autoOpenKeyboard)
    }

    @Test
    fun chosenLayoutAndKeyboardRoundTrip() {
        val chosen =
            NavigationPreferences(
                layout =
                    NavigationLayout(
                        bottomBar = BottomBarButton.entries.reversed(),
                        threads = ThreadsHubItem.entries.reversed(),
                        threadsInMenu = setOf(ThreadsHubItem.HISTORY, ThreadsHubItem.DOWNLOADS),
                    ),
                autoOpenKeyboard = false,
            )
        val preferences = mutablePreferencesOf()
        preferences.writeNavigation(chosen)
        val loaded = preferences.readNavigation()
        assertEquals(BottomBarButton.entries.reversed(), loaded.bottomBarOrder)
        assertEquals(ThreadsHubItem.entries.reversed(), loaded.layout.threads)
        assertEquals(setOf(ThreadsHubItem.HISTORY, ThreadsHubItem.DOWNLOADS), loaded.layout.threadsInMenu)
        assertFalse(loaded.autoOpenKeyboard)
    }
}
