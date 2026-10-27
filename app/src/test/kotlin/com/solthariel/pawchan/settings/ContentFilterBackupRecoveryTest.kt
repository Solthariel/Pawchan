package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.activeContentFilters
import com.solthariel.pawchan.domain.filter.contentFilterValidationError
import com.solthariel.pawchan.domain.filter.isCurrentFilterMode
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentFilterBackupRecoveryTest {
    private val board = defaultBoardAddress()
    private val base = newContentFilterRule(board, "кот", comment = true)
    private val legacy = ContentFilterMode.entries.filterNot { it.isCurrentFilterMode }

    @Test fun restoredLegacyDataIsUnchangedButNeitherEngineNorActiveFlowExecutesIt() =
        runTest {
            for (mode in legacy) {
                val original =
                    base.copy(
                        boards = setOf("b", "vg"),
                        condition = ContentFilterConditionSpec(mode, listOf("кот", "собака", "кот")),
                        options = ContentFilterOptions("  Метка\nправила", false, 12, listOf("исключение\n", "x")),
                    )
                val restored = restore(original)
                assertEquals(original, restored)
                assertFalse(restored.mode.isCurrentFilterMode)
                assertNotNull(contentFilterValidationError(restored))
                assertNull(ContentFilterEngine(listOf(restored)).comment(board, "кот и собака"))
                assertNull(ContentFilterEngine(listOf(restored)).thread(board, "кот", "собака"))
                assertEquals(emptyList<ContentFilterRule>(), flowOf(listOf(restored)).activeContentFilters().first())
            }
        }

    @Test fun legacyImportStillRejectsEmptyOversizedAndStructurallyInvalidRecords() {
        for (mode in legacy) {
            val rule = base.copy(condition = base.condition.copy(mode = mode))
            val invalid =
                listOf(
                    rule.copy(condition = rule.condition.copy(values = emptyList())),
                    rule.copy(condition = rule.condition.copy(values = listOf(" \t"))),
                    rule.copy(condition = rule.condition.copy(values = List(129) { "кот" })),
                    rule.copy(condition = rule.condition.copy(values = listOf("x".repeat(32769)))),
                    rule.copy(options = rule.options.copy(exceptions = List(129) { "x" })),
                    rule.copy(options = rule.options.copy(exceptions = listOf("x".repeat(32769)))),
                    rule.copy(options = rule.options.copy(maxGap = -1)),
                    rule.copy(targets = rule.targets.copy(threads = false, comments = false)),
                    rule.copy(targets = rule.targets.copy(catalog = false, thread = false)),
                )
            invalid.forEach { candidate -> assertThrows(IllegalArgumentException::class.java) { restore(candidate) } }
        }
    }

    @Test fun currentRegexAndExactValidationIsNotBypassedDuringRestore() {
        val invalid =
            listOf(
                ContentFilterConditionSpec(ContentFilterMode.EXACT_PHRASE, listOf("кот", "собака")),
                ContentFilterConditionSpec(ContentFilterMode.REGEX, listOf("(")),
                ContentFilterConditionSpec(ContentFilterMode.REGEX, listOf("a".repeat(4097))),
                ContentFilterConditionSpec(ContentFilterMode.REGEX, listOf("кот", "собака")),
            )
        invalid.forEach { condition ->
            assertThrows(IllegalArgumentException::class.java) { restore(base.copy(condition = condition)) }
        }
    }

    @Test fun validCurrentModesStillExecuteAfterRestore() =
        runTest {
            for (mode in listOf(ContentFilterMode.EXACT_PHRASE, ContentFilterMode.REGEX)) {
                val restored = restore(base.copy(condition = base.condition.copy(mode = mode)))
                assertNull(contentFilterValidationError(restored))
                assertEquals(listOf(restored), flowOf(listOf(restored)).activeContentFilters().first())
                assertNotNull(ContentFilterEngine(listOf(restored)).comment(board, "КОТ"))
            }
        }

    @Test fun legacyRestorationDoesNotBypassAddressAndIdentityChecks() {
        val rule = base.copy(condition = base.condition.copy(mode = ContentFilterMode.ALL_WORDS))
        val invalid =
            listOf(
                rule.copy(id = " "),
                rule.copy(site = ""),
                rule.copy(boards = setOf("not/a/board")),
                rule.copy(targets = rule.targets.copy(threadIds = setOf(-1L))),
            )
        invalid.forEach { candidate -> assertThrows(IllegalArgumentException::class.java) { restore(candidate) } }
    }

    private fun restore(rule: ContentFilterRule): ContentFilterRule =
        decodeContentFilterBackup(encodeContentFilterBackup(listOf(rule))).single()
}
