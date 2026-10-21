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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ComposerCaptchaRaceTest {
    @Test fun cancelledLoadCannotReplaceANewerChallengeOrReportAnOldFailure() =
        runTest {
            for (old in listOf(
                PostingOutcome.Success(PostingCaptchaState(challenge("old"))),
                PostingOutcome.Failure(PostingFailure.NetworkTimeout),
            )) {
                lateinit var pending: Continuation<PostingOutcome<PostingCaptchaState>>
                val base = FakePostingRepository().apply { challenge = challenge("new") }
                var loads = 0
                val posting =
                    object : PostingRepository by base {
                        override suspend fun loadCaptcha(
                            board: BoardAddress,
                            thread: ThreadId?,
                        ): PostingOutcome<PostingCaptchaState> =
                            if (loads++ == 0) suspendCoroutine { pending = it } else base.loadCaptcha(board, thread)
                    }
                val fixture = RaceCaptchaFixture(this, posting)
                fixture.session.load()
                runCurrent()
                fixture.session.load()
                runCurrent()
                pending.resume(old)
                runCurrent()
                assertEquals(
                    "new",
                    fixture.state.captcha.challenge
                        ?.id,
                )
                assertTrue(fixture.failures.isEmpty())
            }
        }

    @Test fun cancelledSelectionCannotPutItsTokenIntoANewChallenge() =
        runTest {
            lateinit var pending: Continuation<PostingOutcome<EmojiCaptchaStep>>
            val base = FakePostingRepository().apply { challenge = challenge("old") }
            val posting =
                object : PostingRepository by base {
                    override suspend fun selectEmoji(
                        challengeId: String,
                        emojiNumber: Int,
                    ): PostingOutcome<EmojiCaptchaStep> = suspendCoroutine { pending = it }
                }
            val fixture = RaceCaptchaFixture(this, posting)
            fixture.session.load()
            runCurrent()
            fixture.session.select(1)
            runCurrent()
            base.challenge = challenge("new")
            fixture.session.load()
            runCurrent()
            pending.resume(PostingOutcome.Success(EmojiCaptchaStep.Solved("stale-token")))
            runCurrent()
            assertEquals(
                "new",
                fixture.state.captcha.challenge
                    ?.id,
            )
            assertNull(fixture.state.captcha.token)
            assertTrue(
                fixture.state.captcha.selectedImages
                    .isEmpty(),
            )
        }

    @Test fun lateRulesCannotStartAnotherCaptchaAfterReload() =
        runTest {
            lateinit var pending: Continuation<PostingOutcome<PostingRules>>
            val base = FakePostingRepository()
            var loads = 0
            val posting =
                object : PostingRepository by base {
                    override suspend fun loadRules(board: BoardAddress): PostingOutcome<PostingRules> =
                        if (loads++ == 0) suspendCoroutine { pending = it } else base.loadRules(board)
                }
            val fixture = RaceCaptchaFixture(this, posting)
            fixture.session.load()
            runCurrent()
            fixture.session.load()
            runCurrent()
            pending.resume(PostingOutcome.Success(PostingRules(maxFiles = 0)))
            runCurrent()
            assertEquals(1, base.captchaLoads)
            assertEquals(4, fixture.state.rules?.maxFiles)
        }

    @Test fun closingSessionRejectsEvenAnUncooperativeResponse() =
        runTest {
            lateinit var pending: Continuation<PostingOutcome<PostingCaptchaState>>
            val posting =
                object : PostingRepository by FakePostingRepository() {
                    override suspend fun loadCaptcha(
                        board: BoardAddress,
                        thread: ThreadId?,
                    ): PostingOutcome<PostingCaptchaState> = suspendCoroutine { pending = it }
                }
            val fixture = RaceCaptchaFixture(this, posting)
            fixture.session.load()
            runCurrent()
            fixture.session.cancel()
            val before = fixture.state
            pending.resume(PostingOutcome.Success(PostingCaptchaState(challenge("late"))))
            runCurrent()
            assertEquals(before, fixture.state)
        }

    @Test fun serverSolvedTokenDoesNotInheritTheChallengeDeadline() =
        runTest {
            var now = 0L
            lateinit var pending: Continuation<PostingOutcome<EmojiCaptchaStep>>
            val base = FakePostingRepository().apply { challenge = challenge("timed") }
            val posting =
                object : PostingRepository by base {
                    override suspend fun selectEmoji(
                        challengeId: String,
                        emojiNumber: Int,
                    ): PostingOutcome<EmojiCaptchaStep> = suspendCoroutine { pending = it }
                }
            val fixture = RaceCaptchaFixture(this, posting) { now }
            fixture.session.load()
            runCurrent()
            fixture.session.select(1)
            runCurrent()
            now = 600_000L
            pending.resume(PostingOutcome.Success(EmojiCaptchaStep.Solved("server-accepted")))
            runCurrent()
            assertTrue(fixture.state.captcha.satisfied)
            assertEquals("server-accepted", fixture.state.captcha.token)
            assertNull(fixture.state.captcha.deadlineElapsedMillis)
        }

    private fun challenge(id: String) =
        EmojiCaptchaChallenge(
            id,
            byteArrayOf(1),
            listOf(EmojiCaptchaOption(1, byteArrayOf(2))),
            300,
        )
}

private class RaceCaptchaFixture(
    scope: CoroutineScope,
    posting: PostingRepository,
    now: () -> Long = { 0L },
) {
    var state = ComposerCaptchaSnapshot(ComposerCaptchaState(), ComposerProgressState(), ComposerFailureState(), null)
    val failures = mutableListOf<PostingOutcome.Failure>()
    val session =
        ComposerCaptchaSession(
            scope,
            posting,
            ComposerCaptchaContext(defaultBoardAddress()),
            ComposerCaptchaBindings({ state }, { state = it }, { failures += it }),
            now,
        )
}
