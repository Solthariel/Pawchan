package com.solthariel.pawchan.settings

import com.solthariel.pawchan.infrastructure.CookieRules
import org.junit.Assert.assertEquals
import org.junit.Test

class CookieEntriesTest {
    @Test
    fun blockedCookieStaysListedSoItCanBeUnblocked() {
        val entries =
            cookieEntries(
                listOf("cf_clearance" to "token"),
                CookieRules(blocked = setOf("usercode_auth"), deleteOnExit = setOf("cf_clearance")),
            )
        assertEquals(
            listOf(
                CookieEntry("cf_clearance", "token", blocked = false, deleteOnExit = true),
                CookieEntry("usercode_auth", null, blocked = true, deleteOnExit = false),
            ),
            entries,
        )
    }

    @Test
    fun maskKeepsAnApproximateLength() {
        assertEquals("••••", maskedCookieValue("ab"))
        assertEquals("••••••", maskedCookieValue("abcdef"))
        assertEquals(16, maskedCookieValue("x".repeat(200)).length)
    }
}
