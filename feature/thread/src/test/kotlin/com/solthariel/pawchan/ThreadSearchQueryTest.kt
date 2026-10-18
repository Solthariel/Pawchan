package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadPostChanges
import com.solthariel.pawchan.thread.ThreadPostVisibility
import com.solthariel.pawchan.thread.ThreadSearchFlag
import com.solthariel.pawchan.thread.compactPostDate
import com.solthariel.pawchan.thread.matchesThreadSearch
import com.solthariel.pawchan.thread.parseThreadSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadSearchQueryTest {
    @Test fun ordinarySearchIncludesNamesIdsAndBody() {
        val post = toolsPost(123, "Привет мир", author = "Длинное Имя")
        assertTrue(matchesThreadSearch(post, parseThreadSearch("привет 123"), null))
        assertTrue(matchesThreadSearch(post, parseThreadSearch("длинное"), null))
        assertFalse(matchesThreadSearch(post, parseThreadSearch("другое"), null))
    }

    @Test fun phrasesAndExclusionsWorkTogether() {
        val post = toolsPost(1, "Привет большой мир")
        assertTrue(matchesThreadSearch(post, parseThreadSearch("\"большой мир\" -чужой"), null))
        assertFalse(matchesThreadSearch(post, parseThreadSearch("привет -большой"), null))
    }

    @Test fun advancedFlagsAreActuallyApplied() {
        val post = toolsPost(1, changes = ThreadPostChanges(own = true, new = true, originalPoster = true))
        for (flag in listOf(
            ThreadSearchFlag.MY,
            ThreadSearchFlag.NEW,
            ThreadSearchFlag.OP,
        )) {
            assertTrue(matchesThreadSearch(post, emptyList(), flag))
        }
        assertFalse(matchesThreadSearch(post, emptyList(), ThreadSearchFlag.DELETED))
        assertTrue(matchesThreadSearch(post, parseThreadSearch(":m :-d"), null))
        assertFalse(matchesThreadSearch(post, parseThreadSearch(":-m"), null))
    }

    @Test
    fun hiddenPostsDoNotMatchEvenEmptyQueryOrMatchingFlag() {
        val raw = toolsPost(1, changes = ThreadPostChanges(own = true))
        val hidden = raw.copy(metadata = raw.metadata.copy(visibility = ThreadPostVisibility(hidden = true)))
        assertFalse(matchesThreadSearch(hidden, emptyList(), null))
        assertFalse(matchesThreadSearch(hidden, emptyList(), ThreadSearchFlag.MY))
        assertTrue(matchesThreadSearch(raw, emptyList(), ThreadSearchFlag.MY))
    }

    @Test fun dateIsCompactWithoutDroppingSeconds() {
        assertEquals("05.10 09:08:06", compactPostDate("05/10/26 Пнд 09:08:06"))
    }
}
