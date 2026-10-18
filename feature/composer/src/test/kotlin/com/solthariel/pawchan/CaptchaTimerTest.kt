package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.captchaTimerLabel
import com.solthariel.pawchan.composer.isExpired
import com.solthariel.pawchan.composer.remainingSeconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptchaTimerTest {
    @Test fun unsupportedLifetimeDoesNotInventATimer() {
        assertNull(ComposerCaptchaState().remainingSeconds(123_000))
        assertFalse(ComposerCaptchaState().isExpired(123_000))
    }

    @Test fun usesDeadlineRatherThanAccumulatingVisibleTicks() {
        val captcha = ComposerCaptchaState(deadlineElapsedMillis = 301_000)
        assertEquals(300L, captcha.remainingSeconds(1_000))
        assertEquals(120L, captcha.remainingSeconds(181_000))
        assertEquals(1L, captcha.remainingSeconds(300_999))
        assertEquals(0L, captcha.remainingSeconds(301_000))
        assertTrue(captcha.isExpired(600_000))
    }

    @Test fun choiceAndRecompositionDoNotResetTheDeadline() {
        val captcha = ComposerCaptchaState(deadlineElapsedMillis = 301_000)
        val updated = captcha.copy(selectedImages = listOf(byteArrayOf(1)))
        assertEquals(captcha.remainingSeconds(31_000), updated.remainingSeconds(31_000))
    }

    @Test fun timerLabelHasNoDotSeparators() {
        assertEquals("5:00", captchaTimerLabel(300))
        assertEquals("0:09", captchaTimerLabel(9))
        assertEquals("0:00", captchaTimerLabel(0))
    }
}
