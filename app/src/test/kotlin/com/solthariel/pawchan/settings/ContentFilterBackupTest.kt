package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterFields
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterBackupTest {
    private val base = newContentFilterRule(defaultBoardAddress(), "Ёжик, хлеб?\n\t= : \\")

    @Test
    fun utf8BackupRoundTripsAllScopesOptionsNullabilityAndDisabledRules() {
        val rule =
            base.copy(
                boards = setOf("b", "vg"),
                enabled = false,
                targets = ContentFilterTargets(true, true, ContentFilterFields.SUBJECT, false, true, setOf(42L)),
                options = ContentFilterOptions("Имя\nправила", false, 12, listOf("x\ny", "z;="), 123456789L),
            )
        val text = encodeContentFilterBackup(listOf(rule, base))
        assertTrue(text.contains("Ёжик"))
        assertEquals(listOf(rule, base), decodeContentFilterBackup(text))
    }

    @Test
    fun everyLegacyModeRetainsItsExactValuesWithoutImplicitConversion() {
        ContentFilterMode.entries.forEach { mode ->
            val values = if (mode == ContentFilterMode.REGEX) listOf("кот|пёс") else listOf("голова")
            val rule = base.copy(condition = ContentFilterConditionSpec(mode, values))
            assertEquals(listOf(rule), decodeContentFilterBackup(encodeContentFilterBackup(listOf(rule))))
        }
    }

    @Test
    fun repeatedRestoreSkipsSemanticDuplicatesAndPreservesExistingOrder() {
        val incoming = base.copy(id = "other", condition = ContentFilterConditionSpec(values = listOf("хлеб")))
        val merged = mergeContentFilterBackup(listOf(base), listOf(base.copy(id = "same-content"), incoming))
        assertEquals(listOf(base, incoming), merged)
        assertEquals(merged, mergeContentFilterBackup(merged, listOf(base, incoming)))
    }

    @Test
    fun conflictingIdentityNeverOverwritesTheExistingRule() {
        val changed = base.copy(boards = setOf("vg"))
        val merged = mergeContentFilterBackup(listOf(base), listOf(changed))
        assertEquals(base, merged.first())
        assertNotEquals(base.id, merged.last().id)
        assertEquals(changed.copy(id = merged.last().id), merged.last())
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedVersionIsRejectedBeforeApplyingAnything() {
        decodeContentFilterBackup("format=pawchan-autohide\nversion=9\nrules=0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun truncatedRuleIsNotSilentlySkipped() {
        decodeContentFilterBackup("format=pawchan-autohide\nversion=1\nrules=1\nrule.0.id=x")
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateFieldsAreRejectedRatherThanOverwritingEarlierValues() {
        decodeContentFilterBackup("format=pawchan-autohide\nversion=1\nrules=0\nrules=1")
    }

    @Test
    fun emptyBackupDoesNotDeleteCurrentRules() {
        val incoming = decodeContentFilterBackup(encodeContentFilterBackup(emptyList()))
        assertEquals(listOf(base), mergeContentFilterBackup(listOf(base), incoming))
    }
}
