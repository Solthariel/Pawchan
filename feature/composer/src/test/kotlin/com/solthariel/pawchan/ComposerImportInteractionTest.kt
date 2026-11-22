package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftActivity
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ReplyComposerState
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerImportInteractionTest {
    @Test fun lateCancellationCannotUnlockANewImportOrOverwriteANewDraft() {
        val initial = ComposerDraftState(comment = "keep")
        val reading = initial.withImportId(7, ComposerDraftActivity.LOADING_FILES)
        val cancelled = initial.withImportId(7)
        assertTrue(initial.acceptsChange(reading))
        assertTrue(reading.acceptsChange(cancelled))
        assertFalse(reading.withImportId(8).acceptsChange(cancelled))
        assertFalse(reading.withResetId(1).acceptsChange(cancelled))
        assertFalse(initial.acceptsChange(cancelled))
    }

    @Test fun preservingAppearanceNeverAllowsSubmissionWhileImporting() {
        val idle =
            ReplyComposerState(
                draft = ComposerDraftState(comment = "Ready to send"),
                captchaState = ComposerCaptchaState(satisfied = true),
                rules = PostingRules(),
            )
        assertTrue(idle.canSubmit)
        assertTrue(idle.canRequestSubmit)
        val loading = idle.copy(draft = idle.draft.copy(activity = ComposerDraftActivity.LOADING_FILES))
        assertTrue(loading.draft.inputBlocked)
        assertFalse(loading.canSubmit)
        assertFalse(loading.canRequestSubmit)
        val finished = loading.copy(draft = loading.draft.copy(activity = ComposerDraftActivity.IDLE))
        assertTrue(finished.canSubmit)
        assertTrue(finished.canRequestSubmit)
    }
}
