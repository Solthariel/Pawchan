package com.solthariel.pawchan.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationPreferencesTest {
    @Test fun defaultsUseFiveVisibleRows() {
        val value = NavigationPreferences()
        assertEquals(listOf(5, 5, 5), listOf(value.pages, value.favorites, value.replies))
        assertEquals(5000, StoragePreferences.DEFAULT_MB)
    }

    @Test fun invalidLimitsAreClampedIndependently() {
        assertEquals(NavigationPreferences(1, 20, 12), NavigationPreferences(-2, 99, 12).normalized())
    }

    @Test fun mirrorsAcceptOnlyDomainNamesNotUrlsOrPaths() {
        listOf("2ch.su", "2ch.org", "2ch.life", "board.example.org").forEach { assertTrue(validMirrorHost(it)) }
        listOf("https://2ch.su", "2ch.su/path", "localhost", "x..org", "-x.org", "X.org", "x.org:443")
            .forEach { assertFalse(it, validMirrorHost(it)) }
    }
}
