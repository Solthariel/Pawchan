package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterThreadScopeTest {
    @Test fun scopesAreConjunctiveAndBlankMeansAll() {
        val base = builderRule()
        assertNotNull(ContentFilterEngine(listOf(base)).comment(builderBoard, "кот", 17))
        val scoped = base.copy(boards = setOf("b"), targets = base.targets.copy(threadIds = setOf(42)))
        val engine = ContentFilterEngine(listOf(scoped))
        assertNotNull(engine.comment(builderBoard, "кот", 42))
        assertNull(engine.comment(builderBoard, "кот", 43))
        assertNull(engine.comment(builderBoard, "кот"))
        val other = BoardAddress(builderBoard.site, BoardId.parse("mobi")!!)
        assertNull(engine.comment(other, "кот", 42))
    }

    @Test fun sameIdsOnOtherSiteNeverMatch() {
        val rule = builderRule()
        val other = BoardAddress(SiteId.parse("other")!!, builderBoard.board)
        assertNull(ContentFilterEngine(listOf(rule)).comment(other, "кот", 42))
    }
}
