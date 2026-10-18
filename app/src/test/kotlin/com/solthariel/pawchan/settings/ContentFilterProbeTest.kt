package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterProbeTest {
    @Test
    fun allWordsReportsTheActualSourceFragmentsRatherThanAnAbstractSuccessLabel() {
        val rule =
            newContentFilterRule(defaultBoardAddress()).copy(
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_WORDS, listOf("голова", "дай", "денег")),
            )
        val result = filterProbeResult(rule, "Денег много; ГОЛОВА, дай немного!")
        assertTrue(result.matched)
        assertTrue(result.caption.contains("«ГОЛОВА»"))
        assertTrue(result.caption.contains("«дай»"))
        assertTrue(result.caption.contains("«Денег»"))
        assertEquals(3, result.preview.spanStyles.size)
    }

    @Test
    fun differentWordFormsDoNotProduceAnInventedMatchingFragment() {
        val rule = newContentFilterRule(defaultBoardAddress(), "голова дай денег")
        val result = filterProbeResult(rule, "головы дайте деньги")
        assertFalse(result.matched)
        assertEquals("Нет совпадений", result.caption)
        assertTrue(result.preview.spanStyles.isEmpty())
    }
}
