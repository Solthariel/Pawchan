package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterValidationTest {
    @Test fun onlyThreeNewModesExecute() {
        for (mode in ContentFilterMode.entries) {
            val rule = builderRule(mode)
            assertEquals(mode.isBuilderMode, contentFilterValidationError(rule) == null)
            assertEquals(mode.isBuilderMode, ContentFilterEngine(listOf(rule)).comment(builderBoard, "кот") != null)
        }
    }

    @Test fun emptyAndOversizeInputsAreRejectedWithoutTruncating() {
        val rule = builderRule()
        for (terms in listOf(emptyList(), listOf("  "), List(129) { "x" }, listOf("x".repeat(32769)))) {
            assertNotNull(contentFilterValidationError(rule.copy(condition = rule.condition.copy(values = terms))))
        }
        assertNotNull(
            contentFilterValidationError(rule.copy(options = rule.options.copy(exceptions = List(129) { "x" }))),
        )
        assertNotNull(
            contentFilterValidationError(
                rule.copy(options = rule.options.copy(exceptions = listOf("x".repeat(32769)))),
            ),
        )
    }

    @Test fun phraseRequiresOneTermAndBuilderRequiresAtLeastOnePostKind() {
        val rule = builderRule(terms = listOf("кот", "собака"))
        assertNotNull(contentFilterValidationError(rule))
        assertNotNull(
            contentFilterValidationError(
                builderRule().copy(targets = ContentFilterTargets(threads = false, comments = false)),
            ),
        )
        assertNull(
            contentFilterValidationError(builderRule(ContentFilterMode.ALL_TERMS, listOf("кот", "собака"))),
        )
    }
}
