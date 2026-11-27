package com.solthariel.pawchan.infrastructure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CookieRulesTest {
    @Test
    fun blockedCookiesAreNeitherAcceptedNorSent() {
        val rules = CookieRules().withBlocked("usercode_auth", true)
        assertEquals(listOf("cf_clearance"), listOf("usercode_auth", "cf_clearance").allowedBy(rules) { it })
        assertEquals(emptySet<String>(), rules.withBlocked("usercode_auth", false).blocked)
    }

    @Test
    fun headerSplitsIntoNamesAndValues() {
        assertEquals(listOf("a" to "1", "b" to "x=y"), parseCookiePairs("a=1; b=x=y; broken; =z"))
        assertEquals(emptyList<Pair<String, String>>(), parseCookiePairs(null))
    }

    @Test
    fun deletionCoversHostAndDomainCookies() {
        val headers = expiredCookieHeaders("a", "2ch.su")
        assertEquals(3, headers.size)
        assertTrue(headers.all { it.startsWith("a=; Max-Age=0") })
        assertTrue(headers.any { it.endsWith("Domain=2ch.su") } && headers.any { it.endsWith("Domain=.2ch.su") })
    }

    @Test
    fun exitSweepDeletesOnlyMarkedCookiesThatExist() {
        val site = FakeSiteCookies(mutableListOf("a" to "1", "b" to "2"))
        site.deleteOnExit(CookieRules(deleteOnExit = setOf("b", "gone")))
        assertEquals(listOf("b"), site.deleted)
    }

    private class FakeSiteCookies(
        private val cookies: MutableList<Pair<String, String>>,
    ) : SiteCookies {
        val deleted = mutableListOf<String>()
        override val host = "2ch.su"

        override fun list(): List<Pair<String, String>> = cookies.toList()

        override fun delete(name: String) {
            deleted += name
            cookies.removeAll { it.first == name }
        }
    }
}
