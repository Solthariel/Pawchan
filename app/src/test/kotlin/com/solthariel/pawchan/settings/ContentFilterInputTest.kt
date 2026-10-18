package com.solthariel.pawchan.settings

import androidx.compose.runtime.saveable.SaverScope
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterFields
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterInputTest {
    private val base = newContentFilterRule(defaultBoardAddress(), "голова дай денег")

    @Test
    fun newRulesUseOneExactPhraseAndFreshIdentityWithoutThreadNumber() {
        assertEquals(ContentFilterMode.EXACT_PHRASE, base.mode)
        assertEquals(listOf("голова дай денег"), base.values)
        assertEquals(setOf(defaultBoardAddress().board.value), base.boards)
        assertTrue(base.threads)
        assertFalse(base.comments)
        val reply = newContentFilterRule(defaultBoardAddress(), "ответ", comment = true)
        assertFalse(reply.threads)
        assertTrue(reply.comments)
        assertNotEquals(base.id, reply.id)
    }

    @Test
    fun simpleAllWordsAcceptsCommasSpacesAndNewlinesWithoutMorphology() {
        val draft = ContentFilterDraft(base)
        draft.selectMode(ContentFilterMode.ALL_WORDS)
        draft.text = "голова, дай\n денег\u00a0хлеб, дай"
        assertEquals(listOf("голова", "дай", "денег", "хлеб", "дай"), draft.rule(base).values)
        assertTrue(draft.rule(base).options.wholeWords)
        draft.selectMode(ContentFilterMode.EXACT_PHRASE)
        assertEquals(listOf(draft.text), draft.rule(base).values)
    }

    @Test
    fun legacyMultiwordAllWordsAndPartialMatchingAreNotSilentlyConverted() {
        val legacy =
            base.copy(
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_WORDS, listOf("два слова", "ещё, пример")),
                options = ContentFilterOptions(wholeWords = false),
            )
        val draft = ContentFilterDraft(legacy)
        assertEquals(legacy, draft.rule(legacy))
        draft.selectMode(ContentFilterMode.ALL_WORDS)
        assertEquals(listOf("два", "слова", "ещё", "пример"), draft.rule(legacy).values)
    }

    @Test
    fun openingLegacyRegexPreservesAllOptionsAndPreciseExpiryTimestamp() {
        val legacy =
            base.copy(
                targets = ContentFilterTargets(comments = true, fields = ContentFilterFields.SUBJECT, catalog = false),
                condition = ContentFilterConditionSpec(ContentFilterMode.REGEX, listOf("храм.*\nголовы")),
                options = ContentFilterOptions("Имя", false, 12, listOf("исключение\nс продолжением"), 123456789L),
            )
        assertEquals(legacy, ContentFilterDraft(legacy).rule(legacy))
    }

    @Test
    fun saveableDraftRestoresBothTargetsModeAndPlacementWithoutChangingIdentity() {
        val draft = ContentFilterDraft(base)
        draft.comments = true
        draft.catalog = false
        draft.selectMode(ContentFilterMode.ALL_WORDS)
        draft.text = "голова, денег"
        val scope =
            object : SaverScope {
                override fun canBeSaved(value: Any): Boolean = true
            }
        val saved = with(ContentFilterDraft.Saver) { checkNotNull(scope.save(draft)) }
        val restored = checkNotNull(ContentFilterDraft.Saver.restore(saved))
        assertEquals(draft.rule(base), restored.rule(base))
        assertEquals(base.id, restored.rule(base).id)
    }

    @Test
    fun searchAndTargetFilterAreIndependent() {
        assertTrue(base.matchesRuleSearch("ГОЛОВА"))
        assertTrue(base.matchesRuleSearch("ГОЛОВА"))
        assertTrue(base.copy(enabled = false).matchesRuleSearch(""))
        assertFalse(base.matchesRuleSearch("нет такого текста"))
    }
}
