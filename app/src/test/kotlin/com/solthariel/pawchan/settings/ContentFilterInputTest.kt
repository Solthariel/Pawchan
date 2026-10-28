package com.solthariel.pawchan.settings

import androidx.compose.runtime.saveable.SaverScope
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterInputTest {
    private val base = newContentFilterRule(defaultBoardAddress(), "голова дай денег")

    @Test fun newRulesApplyToBothPostKindsWithNoRegexMode() {
        assertEquals(ContentFilterMode.PHRASE, base.mode)
        assertEquals(listOf("голова дай денег"), base.values)
        assertTrue(base.threads && base.comments)
        val other = newContentFilterRule(defaultBoardAddress(), "ответ")
        assertNotEquals(base.id, other.id)
    }

    @Test fun rowsKeepTheirCommaSeparatedAlternatives() {
        val draft = ContentFilterDraft(base)
        draft.selectMode(ContentFilterMode.ALL_TERMS)
        draft.text = "два слова\nещё, пример"
        assertEquals(listOf("два слова", "ещё, пример"), draft.rule(base).values)
        draft.selectMode(ContentFilterMode.PHRASE)
        assertEquals(listOf(draft.text), draft.rule(base).values)
    }

    @Test fun saveableDraftKeepsBoundaryExceptionsScopeAndIdentity() {
        val draft = ContentFilterDraft(base)
        draft.selectMode(ContentFilterMode.ANY_TERM)
        draft.text = "кот\nсобака"
        draft.boundary = "PART"
        draft.exceptions = "приют\nдом"
        draft.threadIds = "42, 43"
        draft.postScope = FilterPostScope.COMMENTS.name
        val scope =
            object : SaverScope {
                override fun canBeSaved(value: Any): Boolean = true
            }
        val saved = with(ContentFilterDraft.Saver) { checkNotNull(scope.save(draft)) }
        val restored = checkNotNull(ContentFilterDraft.Saver.restore(saved))
        assertEquals(draft.rule(base), restored.rule(base))
        assertEquals(base.id, restored.rule(base).id)
    }

    @Test fun searchIsCaseInsensitiveAndIndependentOfEnabled() {
        assertTrue(base.matchesRuleSearch("ГОЛОВА"))
        assertTrue(base.copy(enabled = false).matchesRuleSearch(""))
        assertFalse(base.matchesRuleSearch("нет такого текста"))
    }
}
