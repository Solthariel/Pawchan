package com.solthariel.pawchan.domain.boards

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveBoardSearchTest {
    @Test
    fun codeWorksWithoutLoadedDirectory() {
        assertEquals("biz", resolveBoardSearch("biz", emptyList())?.value)
        assertEquals("custom-board", resolveBoardSearch("custom-board", emptyList())?.value)
    }

    @Test
    fun trimsSlashesWhitespaceAndCase() {
        assertEquals("biz", resolveBoardSearch(" /BIZ/ ", emptyList())?.value)
    }

    @Test
    fun exactTitleUsesTheDirectoryBoard() {
        assertEquals("biz", resolveBoardSearch("Бизнес", listOf(entry("biz", "Бизнес")))?.value)
    }

    @Test
    fun exactCodeWinsOverAnotherBoardsTitle() {
        assertEquals("biz", resolveBoardSearch("biz", listOf(entry("biz", "Бизнес"), entry("po", "biz")))?.value)
    }

    @Test
    fun emptyMalformedAndUrlsDoNotNavigate() {
        listOf("", " ", "/", "biz po", "https://2ch.su/biz/", "../biz", "би").forEach {
            assertNull(resolveBoardSearch(it, emptyList()))
        }
    }

    @Test
    fun ambiguousTitleIsNotSilentlyResolvedToFirstResult() {
        assertNull(resolveBoardSearch("Общая тема", listOf(entry("a", "Общая тема"), entry("b", "Общая тема"))))
    }

    @Test
    fun selectedSiteScopesSearchAndPreservesIdentity() {
        val dvach = requireNotNull(SiteId.parse("dvach"))
        val fourChan = requireNotNull(SiteId.parse("4chan"))
        val directory =
            listOf(
                entry("g", "Наука"),
                entry("g", "Technology").copy(address = BoardAddress(fourChan, requireNotNull(BoardId.parse("g")))),
            )
        assertEquals(
            BoardAddress(dvach, requireNotNull(BoardId.parse("g"))),
            resolveBoardAddressSearch("Наука", directory, dvach),
        )
        assertEquals(
            BoardAddress(fourChan, requireNotNull(BoardId.parse("g"))),
            resolveBoardAddressSearch("Technology", directory, fourChan),
        )
        assertNull(resolveBoardAddressSearch("Наука", directory, fourChan))
        assertEquals(
            BoardAddress(fourChan, requireNotNull(BoardId.parse("custom"))),
            resolveBoardAddressSearch("custom", emptyList(), fourChan),
        )
    }

    private fun entry(
        code: String,
        title: String,
    ): BoardDirectoryEntry =
        BoardDirectoryEntry(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse(code))),
            title,
            "",
            0,
            true,
        )
}
