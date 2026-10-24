package com.solthariel.pawchan.settings

import androidx.compose.runtime.saveable.SaverScope
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentFilterModeRoundTripTest {
    private val phrase = "голова дай денег"
    private val base =
        ContentFilterRule(
            "rule",
            "2ch",
            condition = ContentFilterConditionSpec(ContentFilterMode.EXACT_PHRASE, listOf(phrase)),
        )

    @Test
    fun savedWordsCanBeReopenedAsExactPhraseWithoutGeneratedCommas() {
        val first = ContentFilterDraft(base)
        first.selectMode(ContentFilterMode.ALL_WORDS)
        val saved = first.rule(base)
        val reopened = ContentFilterDraft(saved)
        reopened.selectMode(ContentFilterMode.EXACT_PHRASE)
        assertEquals(listOf(phrase), reopened.rule(saved).values)
    }

    @Test
    fun openingSimpleWordsUsesSpacesAndPreservesDuplicateTokens() {
        val saved =
            base.copy(
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_WORDS, listOf("дай", "дай", "денег")),
            )
        val draft = ContentFilterDraft(saved)
        assertEquals("дай дай денег", draft.text)
        assertEquals(saved, draft.rule(saved))
    }

    @Test
    fun userPunctuationIsNotRemovedByModeSwitches() {
        val draft = ContentFilterDraft(base)
        draft.text = "хлеб, молоко; чай!"
        repeat(3) {
            draft.selectMode(ContentFilterMode.ALL_WORDS)
            draft.selectMode(ContentFilterMode.EXACT_PHRASE)
        }
        assertEquals(listOf("хлеб, молоко; чай!"), draft.rule(base).values)
    }

    @Test
    fun restoringReopenedWordsDoesNotReintroduceCommas() {
        val saved = base.copy(condition = ContentFilterConditionSpec(ContentFilterMode.ALL_WORDS, phrase.split(' ')))
        val draft = ContentFilterDraft(saved)
        val scope =
            object : SaverScope {
                override fun canBeSaved(value: Any): Boolean = true
            }
        val state = with(ContentFilterDraft.Saver) { checkNotNull(scope.save(draft)) }
        val restored = checkNotNull(ContentFilterDraft.Saver.restore(state))
        restored.selectMode(ContentFilterMode.EXACT_PHRASE)
        assertEquals(listOf(phrase), restored.rule(saved).values)
    }

    @Test
    fun legacyAtomicValuesAreNotRewrittenOnOpen() {
        val saved =
            base.copy(
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_WORDS, listOf("два слова", "ещё, пример")),
            )
        assertEquals("два слова\nещё, пример", ContentFilterDraft(saved).text)
        assertEquals(saved, ContentFilterDraft(saved).rule(saved))
    }

    @Test
    fun savedExactPhraseRetainsPunctuationAndWhitespace() {
        val saved = base.copy(condition = base.condition.copy(values = listOf("  хлеб,\nмолоко!  ")))
        assertEquals("  хлеб,\nмолоко!  ", ContentFilterDraft(saved).text)
        assertEquals(saved, ContentFilterDraft(saved).rule(saved))
    }
}
