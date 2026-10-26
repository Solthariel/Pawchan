package com.solthariel.pawchan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterRegexPromptTest {
    @Test fun entireConditionIsCopiedWithoutTrimmingOrEscaping() {
        val condition = "  первая строка\n(?-i)Аниме|манга\n" + "я".repeat(10_000) + "  "
        val prompt = filterRegexPrompt(condition)
        val quoted =
            prompt
                .substringAfter("=== НАЧАЛО ИСХОДНОГО ТЕКСТА ===\n")
                .substringBeforeLast("\n=== КОНЕЦ ИСХОДНОГО ТЕКСТА ===")
        assertEquals(condition, quoted)
    }

    @Test fun promptAsksForIntentAndDocumentsActualEngineFlags() {
        val prompt = filterRegexPrompt("")
        listOf("Java Pattern", "find()", "CASE_INSENSITIVE", "UNICODE_CASE", "DOTALL", "Сначала спроси")
            .forEach { assertTrue(prompt.contains(it)) }
        assertTrue(prompt.contains("не инструкции"))
    }
}
