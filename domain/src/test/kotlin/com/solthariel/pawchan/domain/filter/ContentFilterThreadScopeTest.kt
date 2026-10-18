package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterThreadScopeTest {
    @Test
    fun containingThreadNumberRestrictsBothOpAndRepliesWithoutMatchingPostNumbers() {
        val board = BoardAddress(checkNotNull(SiteId.parse("dvach")), checkNotNull(BoardId.parse("b")))
        val rule =
            ContentFilterRule(
                "scope",
                "dvach",
                targets = ContentFilterTargets(comments = true, threadIds = setOf(42)),
                condition = ContentFilterConditionSpec(ContentFilterMode.EXACT_PHRASE, listOf("голова")),
            )
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.thread(board, null, "голова", 42))
        assertNotNull(engine.comment(board, "голова", 42))
        assertNull(engine.thread(board, null, "голова", 43))
        assertNull(engine.comment(board, "голова", 43))
        assertNull(engine.comment(board, "42", 42))
        assertNull(engine.comment(board, "голова"))
    }
}
