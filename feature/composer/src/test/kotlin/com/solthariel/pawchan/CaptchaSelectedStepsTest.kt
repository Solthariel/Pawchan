package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.composer.showCaptchaSelection
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptchaSelectedStepsTest {
    private val captcha = ComposerCaptchaState(selectedImages = listOf(byteArrayOf(1)))

    private fun status(
        progress: ComposerProgressState = ComposerProgressState(),
        failure: ComposerFailureState = ComposerFailureState(),
    ) = ComposerScreenStatus(progress, failure, false)

    @Test fun acceptedStepReplacesIdleLabel() {
        assertTrue(showCaptchaSelection(captcha, status()))
    }

    @Test fun emptyOrSolvedCaptchaStillShowsStatus() {
        assertFalse(showCaptchaSelection(ComposerCaptchaState(), status()))
        assertFalse(showCaptchaSelection(captcha.copy(satisfied = true), status()))
    }

    @Test fun pendingAndSubmittingStatesHavePriority() {
        listOf(
            ComposerProgressState(captchaLoading = true),
            ComposerProgressState(captchaSelecting = true),
            ComposerProgressState(submitting = true),
            ComposerProgressState(sent = true),
        ).forEach {
            assertFalse(showCaptchaSelection(captcha, status(it)))
        }
    }

    @Test fun errorOrServerMessageIsNeverReplacedByImages() {
        assertFalse(showCaptchaSelection(captcha, status(failure = ComposerFailureState(ReplyError.CAPTCHA))))
        assertFalse(showCaptchaSelection(captcha, status(failure = ComposerFailureState(reason = "error"))))
    }
}
