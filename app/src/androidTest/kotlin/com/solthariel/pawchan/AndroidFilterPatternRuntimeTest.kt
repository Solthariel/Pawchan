package com.solthariel.pawchan

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AndroidFilterPatternRuntimeTest {
    @Test fun unicodeWordBoundariesAndWholeFieldExceptionWorkOnAndroid() {
        val word = "[\\p{L}\\p{M}\\p{N}_]"
        val pattern =
            "\\A(?!.*(?<!$word)приют(?!$word))" +
                "(?=.*(?<!$word)кот(?!$word))(?=.*(?<!$word)собака(?!$word)).+"
        val engine = ContentFilterEngine(listOf(rule(pattern)))
        val board = defaultBoardAddress()
        assertNotNull(engine.comment(board, "КОТ\nсобака"))
        assertNull(engine.comment(board, "кот собака\nПРИЮТ"))
        assertNull(engine.comment(board, "кот собака_1"))
        assertNull(engine.comment(board, "Xкот собака"))
    }

    @Test fun unsupportedInlineFlagIsReportedWithoutEnglishError() {
        val error = contentFilterValidationError(rule("(?U)кот"))
        assertNotNull(error)
        org.junit.Assert.assertFalse(checkNotNull(error).contains("Invalid"))
    }

    private fun rule(expression: String): ContentFilterRule =
        ContentFilterRule(
            "r",
            "dvach",
            targets = ContentFilterTargets(comments = true),
            condition = ContentFilterConditionSpec(ContentFilterMode.REGEX, listOf(expression)),
        )
}
