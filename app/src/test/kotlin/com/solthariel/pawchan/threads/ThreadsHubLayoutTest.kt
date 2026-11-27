package com.solthariel.pawchan.threads

import com.solthariel.pawchan.domain.settings.NavigationLayout
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.domain.settings.ThreadsHubItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadsHubLayoutTest {
    @Test
    fun defaultScreenIsTheCurrentOne() {
        assertEquals(ThreadsHubItem.entries, threadsScreenItems(NavigationLayout()))
        assertEquals(emptyList<ThreadsHubItem>(), threadsMenuItems(NavigationLayout()))
    }

    @Test
    fun linksCanMoveUpOrIntoTheMenuButSectionsStayOnScreen() {
        val layout =
            NavigationLayout(
                threads = listOf(ThreadsHubItem.HISTORY, ThreadsHubItem.DOWNLOADS, ThreadsHubItem.PAGES),
                threadsInMenu = setOf(ThreadsHubItem.DOWNLOADS, ThreadsHubItem.PAGES),
            )
        assertEquals(
            listOf(
                ThreadsHubItem.HISTORY,
                ThreadsHubItem.PAGES,
                ThreadsHubItem.FAVORITES,
                ThreadsHubItem.REPLIES,
                ThreadsHubItem.COPIES,
            ),
            threadsScreenItems(layout),
        )
        assertEquals(listOf(ThreadsHubItem.DOWNLOADS), threadsMenuItems(layout))
    }

    @Test
    fun resetTouchesOnlyTheThreadsScreen() {
        val changed =
            NavigationPreferences(
                pages = 2,
                bottomBarTransparencyPercent = 70,
                layout =
                    NavigationLayout(
                        threads = ThreadsHubItem.entries.reversed(),
                        threadsInMenu = setOf(ThreadsHubItem.COPIES),
                    ),
                autoOpenKeyboard = false,
            )
        val reset = changed.withThreadsDefaults()
        assertEquals(NavigationPreferences().pages, reset.pages)
        assertEquals(ThreadsHubItem.entries, reset.layout.threads)
        assertEquals(emptySet<ThreadsHubItem>(), reset.layout.threadsInMenu)
        assertEquals(70, reset.bottomBarTransparencyPercent)
        assertEquals(false, reset.autoOpenKeyboard)
    }

    @Test
    fun onlyFirstThreeItemsAreSections() {
        assertEquals(
            listOf(ThreadsSection.PAGES, ThreadsSection.FAVORITES, ThreadsSection.REPLIES, null, null, null),
            ThreadsHubItem.entries.map { it.section() },
        )
    }
}
