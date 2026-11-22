package com.solthariel.pawchan

import com.solthariel.pawchan.composer.CaptchaSendAction
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerScreenState
import com.solthariel.pawchan.composer.ComposerScreenStatus
import com.solthariel.pawchan.composer.ComposerSendFlow
import com.solthariel.pawchan.composer.NewThreadUiState
import com.solthariel.pawchan.composer.ReplyComposerState
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.composer.captchaSendAction
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import com.solthariel.pawchan.domain.posting.PostingRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerSendFlowTest {
    private fun state(
        captcha: ComposerCaptchaState = ComposerCaptchaState(),
        ready: Boolean = false,
    ) = ComposerScreenState(
        ComposerDraftState(comment = "text"),
        PostingRules(),
        captcha,
        ComposerScreenStatus(ComposerProgressState(), ComposerFailureState(), ready, canRequestSubmit = true),
    )

    @Test fun livePreloadIsShownWithoutAnotherLoad() {
        val challenge = EmojiCaptchaChallenge("id", byteArrayOf(1), listOf(EmojiCaptchaOption(0, byteArrayOf(1))))
        val captcha = ComposerCaptchaState(challenge = challenge, deadlineElapsedMillis = 1000)
        val flow = ComposerSendFlow()
        assertEquals(CaptchaSendAction.SHOW, flow.open(state(captcha), 999))
        assertTrue(flow.visible)
        assertTrue(flow.pending)
        assertNull(flow.suppressedCaptcha)
    }

    @Test fun expiredPreloadIsSuppressedBeforeRefreshing() {
        val captcha = ComposerCaptchaState(deadlineElapsedMillis = 1000)
        val flow = ComposerSendFlow()
        assertEquals(CaptchaSendAction.RELOAD, flow.open(state(captcha), 1000))
        assertSame(captcha, flow.suppressedCaptcha)
    }

    @Test fun unknownLifetimeAndPasscodeDoNotInventExpiry() {
        assertEquals(
            CaptchaSendAction.SEND,
            captchaSendAction(state(ComposerCaptchaState(satisfied = true), true), Long.MAX_VALUE),
        )
    }

    @Test fun runningPreloadIsAwaitedWithoutDuplicatingTheRequest() {
        val s =
            state().let {
                it.copy(status = it.status.copy(progress = ComposerProgressState(captchaLoading = true)))
            }
        assertEquals(CaptchaSendAction.WAIT, ComposerSendFlow().open(s, 5000))
    }

    @Test fun oneShotIntentIsCancelledBeforeLateSolutionsCanSend() {
        val flow = ComposerSendFlow()
        assertFalse(flow.consume())
        flow.open(state(), 0)
        assertTrue(flow.consume())
        assertFalse(flow.consume())
        flow.arm()
        flow.dismiss()
        flow.arm()
        assertFalse(flow.visible)
        assertFalse(flow.consume())
    }

    @Test fun unconfirmedDeliveryAndCooldownCannotTriggerAnExpiredCaptchaReload() {
        val expired = state(ComposerCaptchaState(deadlineElapsedMillis = 1))
        val unconfirmed =
            expired.copy(
                status =
                    expired.status.copy(failure = ComposerFailureState(ReplyError.SUBMISSION_UNCONFIRMED)),
            )
        val flow = ComposerSendFlow()
        assertEquals(CaptchaSendAction.SHOW, flow.open(unconfirmed, 50))
        assertFalse(flow.pending)
        val cooldown =
            expired.copy(
                status =
                    expired.status.copy(
                        failure = ComposerFailureState(ReplyError.SERVER, retryDeadlineElapsedMillis = 100),
                    ),
            )
        assertEquals(CaptchaSendAction.SHOW, flow.open(cooldown, 50))
    }

    @Test fun requestBypassesOnlyCaptchaNotDraftValidationOrBusyStates() {
        val s = ReplyComposerState(visible = true, draft = ComposerDraftState(comment = "text"), rules = PostingRules())
        assertTrue(s.canRequestSubmit)
        assertFalse(s.canSubmit)
        assertFalse(s.copy(draft = ComposerDraftState()).canRequestSubmit)
        assertFalse(s.copy(rules = null).canRequestSubmit)
        assertFalse(s.copy(progress = ComposerProgressState(submitting = true)).canRequestSubmit)
        assertFalse(s.copy(progress = ComposerProgressState(sent = true)).canRequestSubmit)
        assertFalse(s.copy(failure = ComposerFailureState(ReplyError.THREAD_CLOSED)).canRequestSubmit)
        assertFalse(NewThreadUiState(draft = s.draft, rules = PostingRules()).canRequestSubmit)
    }
}
