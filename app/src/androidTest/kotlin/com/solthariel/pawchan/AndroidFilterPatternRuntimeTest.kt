package com.solthariel.pawchan

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidFilterPatternRuntimeTest {
    @Test fun literalUnicodeBoundariesAndWholePostExceptionWorkOnAndroid() {
        val rule =
            ContentFilterRule(
                "r",
                "dvach",
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_TERMS, listOf("кот", "собака")),
                options = ContentFilterOptions(exceptions = listOf("приют")),
            )
        val engine = ContentFilterEngine(listOf(rule))
        val board = defaultBoardAddress()
        assertNotNull(engine.comment(board, "КОТ\nсобака"))
        assertNull(engine.comment(board, "кот собака\nПРИЮТ"))
        assertNull(engine.comment(board, "кот собака_1"))
        assertNull(engine.comment(board, "Xкот собака"))
        assertNotNull(engine.thread(board, "кот", "собака"))
        assertNull(engine.thread(board, "кот", "собака\nприют"))
    }
}
