package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.settings.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceTest {
    @Test
    fun unknownAndMissingValuesFallBackToSystem() {
        assertEquals(ThemePreference.SYSTEM, parseTheme(null))
        assertEquals(ThemePreference.SYSTEM, parseTheme("UNKNOWN"))
    }

    @Test
    fun persistedValueIsParsedExactly() {
        assertEquals(ThemePreference.AMOLED, parseTheme("AMOLED"))
    }

    @Test
    fun boardPreferencesIgnoreMalformedValues() {
        assertEquals(
            setOf("b", "po"),
            parseFavoriteBoards(setOf("b", "bad code", "po")).map { it.value }.toSet(),
        )
    }

    @Test
    fun homeBoardPreservesFullAddressAndRejectsBrokenValues() {
        val board = requireNotNull(parseHomeBoard("dvach/biz"))
        assertEquals("dvach", board.site.value)
        assertEquals("biz", board.board.value)
        listOf(null, "", "b", "dvach/", "dvach/biz/42", "bad site/biz").forEach {
            assertEquals(null, parseHomeBoard(it))
        }
    }
}
