package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerCaptchaBindings
import com.solthariel.pawchan.composer.ComposerCaptchaContext
import com.solthariel.pawchan.composer.ComposerCaptchaSession
import com.solthariel.pawchan.composer.ComposerCaptchaSnapshot
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.PostingCaptchaState
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.posting.PostingRules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComposerCaptchaSessionTest {
    @Test
    fun preservesReplyContextAndSupportsCaptchaSkip() =
        runTest {
            val thread = requireNotNull(ThreadId.parse(42))
            var received: ThreadId? = null
            val base = FakePostingRepository()
            val posting =
                object : PostingRepository by base {
                    override suspend fun loadCaptcha(
                        board: BoardAddress,
                        thread: ThreadId?,
                    ): PostingOutcome<PostingCaptchaState> {
                        received = thread
                        return base.loadCaptcha(board, thread)
                    }
                }
            val fixture = CaptchaSessionFixture(this, posting, thread)
            fixture.session.load()
            runCurrent()
            assertEquals(thread, received)
            assertTrue(fixture.state.captcha.satisfied)
            assertEquals(false, fixture.state.progress.captchaLoading)
        }

    @Test
    fun accumulatesEmojiStepsAndUsesTheSolvedToken() =
        runTest {
            val posting =
                FakePostingRepository().apply {
                    challenge =
                        EmojiCaptchaChallenge(
                            "id",
                            byteArrayOf(1),
                            listOf(EmojiCaptchaOption(1, byteArrayOf(2))),
                        )
                    nextStep = EmojiCaptchaStep.Challenge(byteArrayOf(3), listOf(EmojiCaptchaOption(2, byteArrayOf(4))))
                }
            val fixture = CaptchaSessionFixture(this, posting)
            fixture.session.load()
            runCurrent()
            fixture.session.select(1)
            runCurrent()
            assertEquals(
                "id",
                fixture.state.captcha.challenge
                    ?.id,
            )
            assertEquals(1, fixture.state.captcha.selectedImages.size)
            posting.nextStep = EmojiCaptchaStep.Solved("done")
            fixture.session.select(2)
            runCurrent()
            assertEquals("done", fixture.state.captcha.token)
            assertEquals(2, fixture.state.captcha.selectedImages.size)
            assertTrue(fixture.state.captcha.satisfied)
        }

    @Test fun timedChallengeKeepsDeadlineAcrossStepsAndStopsTimerOnSolve() =
        runTest {
            var now = 1_000L
            val posting =
                FakePostingRepository().apply {
                    challenge = timedChallenge()
                    nextStep = EmojiCaptchaStep.Challenge(byteArrayOf(3), listOf(EmojiCaptchaOption(2, byteArrayOf(4))))
                }
            val fixture = CaptchaSessionFixture(this, posting, nowMillis = { now })
            fixture.session.load()
            runCurrent()
            assertEquals(301_000L, fixture.state.captcha.deadlineElapsedMillis)
            now = 100_000L
            fixture.session.select(1)
            runCurrent()
            assertEquals(301_000L, fixture.state.captcha.deadlineElapsedMillis)
            posting.nextStep = EmojiCaptchaStep.Solved("done")
            fixture.session.select(2)
            runCurrent()
            assertEquals(null, fixture.state.captcha.deadlineElapsedMillis)
            assertTrue(fixture.state.captcha.satisfied)
        }

    @Test fun expiredChallengeRequiresExplicitReloadWithoutSendingAnotherChoice() =
        runTest {
            var now = 1_000L
            val posting =
                FakePostingRepository().apply {
                    challenge = timedChallenge()
                }
            val fixture = CaptchaSessionFixture(this, posting, nowMillis = { now })
            fixture.session.load()
            runCurrent()
            now = 301_000L
            fixture.session.select(1)
            runCurrent()
            assertEquals(listOf(PostingOutcome.Failure(PostingFailure.CaptchaRejected)), fixture.failures)
            assertEquals(0, fixture.state.captcha.selectedImages.size)
            fixture.session.load()
            runCurrent()
            assertEquals(601_000L, fixture.state.captcha.deadlineElapsedMillis)
        }

    @Test
    fun reloadCancelsPreviousRequestAndDoesNotPublishStaleState() =
        runTest {
            val started = CompletableDeferred<Unit>()
            var cancelled = false
            var loads = 0
            val base = FakePostingRepository()
            val posting =
                object : PostingRepository by base {
                    override suspend fun loadCaptcha(
                        board: BoardAddress,
                        thread: ThreadId?,
                    ): PostingOutcome<PostingCaptchaState> {
                        loads++
                        if (loads == 1) {
                            started.complete(Unit)
                            try {
                                awaitCancellation()
                            } finally {
                                cancelled = true
                            }
                        }
                        return base.loadCaptcha(board, thread)
                    }
                }
            val fixture = CaptchaSessionFixture(this, posting)
            fixture.session.load()
            started.await()
            fixture.session.load()
            runCurrent()
            assertTrue(cancelled)
            assertEquals(2, loads)
            assertTrue(fixture.state.captcha.satisfied)
        }

    @Test
    fun ruleFailureDoesNotStartCaptchaAndIsDeliveredToTheOwner() =
        runTest {
            val base = FakePostingRepository()
            val posting =
                object : PostingRepository by base {
                    override suspend fun loadRules(board: BoardAddress): PostingOutcome<PostingRules> =
                        PostingOutcome.Failure(PostingFailure.NetworkUnavailable)
                }
            val fixture = CaptchaSessionFixture(this, posting)
            fixture.session.load()
            runCurrent()
            assertEquals(0, base.captchaLoads)
            assertEquals(listOf(PostingOutcome.Failure(PostingFailure.NetworkUnavailable)), fixture.failures)
        }

    private fun timedChallenge(): EmojiCaptchaChallenge {
        val options = listOf(EmojiCaptchaOption(1, byteArrayOf(2)))
        return EmojiCaptchaChallenge("id", byteArrayOf(1), options, 300)
    }
}

private class CaptchaSessionFixture(
    scope: CoroutineScope,
    posting: PostingRepository,
    thread: ThreadId? = null,
    nowMillis: () -> Long = { 0L },
) {
    var state = ComposerCaptchaSnapshot(ComposerCaptchaState(), ComposerProgressState(), ComposerFailureState(), null)
    val failures = mutableListOf<PostingOutcome.Failure>()
    val session =
        ComposerCaptchaSession(
            scope,
            posting,
            ComposerCaptchaContext(defaultBoardAddress(), thread),
            ComposerCaptchaBindings({ state }, { state = it }, { failures += it }),
            nowMillis,
            preparationDispatcher = Dispatchers.Unconfined,
        )
}
