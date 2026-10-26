package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerSubmissionGuard
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.composer.composerStatusText
import com.solthariel.pawchan.composer.toReplyError
import com.solthariel.pawchan.domain.posting.PostingFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerSubmissionGuardTest {
    @Test
    fun anUnconfirmedAttemptIsNotSilentlyClearedByOtherFailures() {
        val guard = ComposerSubmissionGuard()
        guard.record(PostingFailure.SubmissionUnconfirmed)
        guard.record(PostingFailure.NetworkUnavailable)
        assertEquals(ReplyError.SUBMISSION_UNCONFIRMED, guard.activeFailure()?.error)
    }

    @Test
    fun knownFailuresDoNotCreateAnUnconfirmedAttempt() {
        val guard = ComposerSubmissionGuard()
        guard.record(PostingFailure.CaptchaRejected)
        guard.record(PostingFailure.HttpStatus(429, 30_000L))
        assertNull(guard.activeFailure())
    }

    @Test
    fun acknowledgementClearsOnlyThisFormAndCanBeRepeated() {
        val first = ComposerSubmissionGuard()
        val second = ComposerSubmissionGuard()
        first.record(PostingFailure.SubmissionUnconfirmed)
        second.record(PostingFailure.SubmissionUnconfirmed)
        first.acknowledge()
        first.acknowledge()
        assertNull(first.activeFailure())
        assertEquals(ReplyError.SUBMISSION_UNCONFIRMED, second.activeFailure()?.error)
    }

    @Test
    fun theWarningSaysUnconfirmedAndDoesNotClaimFailureOrSuccess() {
        val failure = ComposerFailureState(ReplyError.SUBMISSION_UNCONFIRMED)
        val status = composerStatusText(ComposerProgressState(), failure, true)
        assertTrue(status.failed)
        assertEquals(
            "Отправка не подтверждена. Проверьте, появилось ли сообщение, перед повторной попыткой.",
            status.label,
        )
        assertFalse(status.label.contains("отклон"))
        assertEquals(ReplyError.SUBMISSION_UNCONFIRMED to null, PostingFailure.SubmissionUnconfirmed.toReplyError())
    }

    @Test
    fun confirmedProgressStillHasPriorityOverOldFailureText() {
        val failure = ComposerFailureState(ReplyError.SUBMISSION_UNCONFIRMED)
        val status = composerStatusText(ComposerProgressState(sent = true), failure, true)
        assertEquals("Сообщение отправлено", status.label)
        assertFalse(status.failed)
    }
}
