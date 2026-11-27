package com.solthariel.pawchan.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationLayoutTest {
    @Test
    fun threadsKeepTheCurrentOrderByDefault() {
        assertEquals(
            listOf(
                ThreadsHubItem.PAGES,
                ThreadsHubItem.FAVORITES,
                ThreadsHubItem.REPLIES,
                ThreadsHubItem.HISTORY,
                ThreadsHubItem.COPIES,
                ThreadsHubItem.DOWNLOADS,
            ),
            NavigationPreferences().normalized().layout.threads,
        )
        assertEquals(emptySet<ThreadsHubItem>(), NavigationPreferences().normalized().layout.threadsInMenu)
    }

    @Test
    fun brokenOrderKeepsEverySectionOnceAndOnlyLinksGoToTheMenu() {
        val layout =
            NavigationLayout(
                threads = parseThreadsHubItems("downloads,pages,downloads,unknown"),
                threadsInMenu = setOf(ThreadsHubItem.PAGES, ThreadsHubItem.HISTORY),
            ).normalized()
        assertEquals(
            listOf(
                ThreadsHubItem.DOWNLOADS,
                ThreadsHubItem.PAGES,
                ThreadsHubItem.FAVORITES,
                ThreadsHubItem.REPLIES,
                ThreadsHubItem.HISTORY,
                ThreadsHubItem.COPIES,
            ),
            layout.threads,
        )
        assertEquals(setOf(ThreadsHubItem.HISTORY), layout.threadsInMenu)
    }

    @Test
    fun storedValueRoundTrips() {
        val order = ThreadsHubItem.entries.reversed()
        assertEquals(order, parseThreadsHubItems(order.threadsHubValue()))
        assertEquals(emptyList<ThreadsHubItem>(), parseThreadsHubItems(""))
    }
}
