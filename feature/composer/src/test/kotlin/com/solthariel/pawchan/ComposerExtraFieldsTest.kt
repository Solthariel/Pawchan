package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.availableComposerFields
import com.solthariel.pawchan.composer.fieldValue
import com.solthariel.pawchan.composer.filledComposerFieldCount
import com.solthariel.pawchan.composer.withFieldValue
import com.solthariel.pawchan.domain.posting.PostingFeatures
import com.solthariel.pawchan.domain.posting.PostingIdentityRules
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.ComposerField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerExtraFieldsTest {
    private val rules = PostingRules(features = PostingFeatures(tags = true))

    @Test fun boardCapabilitiesAndReplyModeHideUnavailableFields() {
        val noName = PostingRules(identity = PostingIdentityRules(namesEnabled = false))
        assertEquals(listOf(ComposerField.EMAIL), availableComposerFields(noName, false, emptySet()))
        assertEquals(
            listOf(ComposerField.SUBJECT, ComposerField.TAG, ComposerField.NAME, ComposerField.EMAIL),
            availableComposerFields(rules, true, emptySet()),
        )
        assertFalse(availableComposerFields(rules, false, emptySet()).contains(ComposerField.TAG))
        assertTrue(availableComposerFields(null, true, emptySet()).isEmpty())
    }

    @Test fun hiddenFieldsAndBlankValuesAreNotCounted() {
        val fields = availableComposerFields(rules, true, setOf(ComposerField.NAME, ComposerField.EMAIL))
        val draft = ComposerDraftState(email = "mail").withFieldValue(ComposerField.NAME, "saved")
        assertEquals(0, filledComposerFieldCount(draft, fields))
        assertEquals(0, filledComposerFieldCount(ComposerDraftState(email = "  "), listOf(ComposerField.EMAIL)))
    }

    @Test fun eachTrashActionClearsOnlyItsFieldAndTheCountIsLive() {
        val fields = availableComposerFields(rules, true, emptySet())
        var draft = ComposerDraftState(comment = "keep")
        for (field in fields) draft = draft.withFieldValue(field, field.name)
        assertEquals(4, filledComposerFieldCount(draft, fields))
        for (field in fields) {
            val cleared = draft.withFieldValue(field, "")
            assertEquals(3, filledComposerFieldCount(cleared, fields))
            assertEquals("keep", cleared.comment)
            for (other in fields.filterNot { it == field }) {
                assertEquals(draft.fieldValue(other), cleared.fieldValue(other))
            }
        }
    }
}
