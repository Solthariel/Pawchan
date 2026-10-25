package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.composer.ComposerRateLimit
import com.solthariel.pawchan.composer.ReplyError
import com.solthariel.pawchan.composer.composerStatusText
import com.solthariel.pawchan.composer.cooldownRemainingSeconds
import com.solthariel.pawchan.domain.posting.PostingFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerRateLimitTest {
    @Test
    fun deadlineCountsDownFromAMonotonicClockWithoutRestartingOnRead() {
        var now = 10_000L
        val limit = ComposerRateLimit { now }
        val first = limit.failure(PostingFailure.HttpStatus(429, 30_000L))
        assertEquals(40_000L, first.retryDeadlineElapsedMillis)
        now = 12_500L
        assertEquals(40_000L, limit.activeFailure()?.retryDeadlineElapsedMillis)
        assertEquals(28L, cooldownRemainingSeconds(40_000L, now))
        now = 40_000L
        assertNull(limit.activeFailure())
    }

    @Test
    fun shorterSubsequentPauseCannotShortenAnExistingDeadline() {
        var now = 0L
        val limit = ComposerRateLimit { now }
        limit.failure(PostingFailure.HttpStatus(429, 60_000L))
        now = 5_000L
        assertEquals(60_000L, limit.failure(PostingFailure.HttpStatus(429, 1_000L)).retryDeadlineElapsedMillis)
        assertEquals(125_000L, limit.failure(PostingFailure.HttpStatus(429, 120_000L)).retryDeadlineElapsedMillis)
    }

    @Test
    fun unknownDurationDoesNotInventATimerAndOtherErrorsKeepTheirMeaning() {
        val limit = ComposerRateLimit { 0L }
        val failure = limit.failure(PostingFailure.HttpStatus(429))
        assertNull(failure.retryDeadlineElapsedMillis)
        assertEquals("Сервер ограничил запросы. Повторите позже", failure.reason)
        assertEquals(ComposerFailureState(ReplyError.TIMEOUT), limit.failure(PostingFailure.NetworkTimeout))
        assertEquals(ComposerFailureState(ReplyError.SERVER), limit.failure(PostingFailure.HttpStatus(503)))
    }

    @Test
    fun separateComposersDoNotShareMutableUiState() {
        val first = ComposerRateLimit { 0L }
        val other = ComposerRateLimit { 0L }
        first.failure(PostingFailure.HttpStatus(429, 30_000L))
        assertNotNull(first.activeFailure())
        assertNull(other.activeFailure())
    }

    @Test
    fun durationAndCeilingDoNotOverflowForExtremeRetryAfter() {
        val limit = ComposerRateLimit { 10L }
        val until =
            requireNotNull(limit.failure(PostingFailure.HttpStatus(429, Long.MAX_VALUE)).retryDeadlineElapsedMillis)
        assertEquals(Long.MAX_VALUE, until)
        assertEquals(9_223_372_036_854_776L, cooldownRemainingSeconds(Long.MAX_VALUE, 0L))
        assertEquals(1L, cooldownRemainingSeconds(1L, 0L))
        assertEquals(0L, cooldownRemainingSeconds(1L, 2L))
    }

    @Test
    fun presentationShowsRemainingWaitAndManualRecoveryWithoutPretendingSuccess() {
        val failure = ComposerFailureState(ReplyError.SERVER, retryDeadlineElapsedMillis = 10_000L)
        val waiting = composerStatusText(ComposerProgressState(), failure, false, cooldownSeconds = 9L)
        assertEquals("Сервер ограничил запросы. Повторить через 9 с", waiting.label)
        assertTrue(waiting.failed)
        assertEquals(
            "Ожидание завершено. Можно повторить загрузку капчи",
            composerStatusText(ComposerProgressState(), failure, false, cooldownSeconds = 0L).label,
        )
        assertEquals(
            "Сообщение отправлено",
            composerStatusText(ComposerProgressState(sent = true), failure, false, cooldownSeconds = 9L).label,
        )
    }
}
