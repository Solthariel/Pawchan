package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.FilterBoundary
import com.solthariel.pawchan.domain.filter.FilterTextMatch
import com.solthariel.pawchan.domain.filter.isBuilderMode
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ContentFilterBackupRecoveryTest {
    private val base = newContentFilterRule(defaultBoardAddress(), "кот")

    @Test fun oldBackupVersionsAreRejectedWithoutApplyingAnything() {
        for (version in listOf(1, 2)) {
            assertThrows(IllegalArgumentException::class.java) {
                decodeContentFilterBackup("format=pawchan-autohide\nversion=$version\nrules=0")
            }
        }
    }

    @Test fun oldModesCannotBeExportedAsNewRules() {
        ContentFilterMode.entries.filterNot { it.isBuilderMode }.forEach { mode ->
            val old = base.copy(condition = ContentFilterConditionSpec(mode, listOf("кот")))
            assertThrows(IllegalArgumentException::class.java) { encodeContentFilterBackup(listOf(old)) }
        }
    }

    @Test fun flagsAndExceptionsSurviveRestoreExactly() {
        for (boundary in listOf(FilterBoundary.WORD, FilterBoundary.PART)) {
            val rule =
                base.copy(
                    options =
                        base.options.copy(
                            exceptions = listOf("приют"),
                            textMatch = FilterTextMatch(boundary, true),
                        ),
                )
            assertEquals(listOf(rule), decodeContentFilterBackup(encodeContentFilterBackup(listOf(rule))))
        }
    }

    @Test fun malformedOrMissingNewFlagCannotFallBackToOldSemantics() {
        val text = encodeContentFilterBackup(listOf(base))
        for (broken in listOf(
            text.replace("caseSensitive=false", "caseSensitive=maybe"),
            text.lines().filterNot { it.contains(".boundary=") }.joinToString("\n"),
        )) {
            assertThrows(IllegalArgumentException::class.java) { decodeContentFilterBackup(broken) }
        }
    }
}
