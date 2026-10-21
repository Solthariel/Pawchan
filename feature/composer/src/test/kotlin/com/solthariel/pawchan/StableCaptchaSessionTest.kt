package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerCaptchaBindings
import com.solthariel.pawchan.composer.ComposerCaptchaContext
import com.solthariel.pawchan.composer.ComposerCaptchaSession
import com.solthariel.pawchan.composer.ComposerCaptchaSnapshot
import com.solthariel.pawchan.composer.ComposerCaptchaState
import com.solthariel.pawchan.composer.ComposerFailureState
import com.solthariel.pawchan.composer.ComposerProgressState
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.PostingOutcome
import com.solthariel.pawchan.domain.posting.PostingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StableCaptchaSessionTest {
    @Test fun unchangedTaskAndButtonsReuseModelsButClicksUseCurrentServerNumbers() =
        runTest {
            val task = byteArrayOf(9)
            val base = FakePostingRepository().apply { challenge = challenge(task) }
            val clicked = mutableListOf<Int>()
            val posting =
                object : PostingRepository by base {
                    override suspend fun selectEmoji(
                        challengeId: String,
                        emojiNumber: Int,
                    ): PostingOutcome<EmojiCaptchaStep> {
                        clicked += emojiNumber
                        return PostingOutcome.Success(
                            EmojiCaptchaStep.Challenge(
                                byteArrayOf(9),
                                listOf(option(0, 3), option(1, 1), option(2, 2)),
                            ),
                        )
                    }
                }
            val fixture = StableCaptchaFixture(this, posting)
            fixture.session.load()
            runCurrent()
            val original = requireNotNull(fixture.state.captcha.challenge)
            fixture.session.select(0)
            advanceUntilIdle()
            assertSame(
                task,
                fixture.state.captcha.challenge
                    ?.taskImage,
            )
            assertSame(
                original.options[0].image,
                fixture.state.captcha.keyboard[0]
                    ?.image,
            )
            assertEquals(
                listOf(1, 2, 0),
                fixture.state.captcha.keyboard
                    .map { it?.number },
            )
            fixture.session.select(requireNotNull(fixture.state.captcha.keyboard[0]).number)
            advanceUntilIdle()
            assertEquals(listOf(0, 1), clicked)
            assertEquals(2, fixture.state.captcha.selectedImages.size)
            assertEquals(301_000L, fixture.state.captcha.deadlineElapsedMillis)
        }

    @Test fun pendingSelectionKeepsExistingContentAndRejectsDoubleClick() =
        runTest {
            val response = CompletableDeferred<PostingOutcome<EmojiCaptchaStep>>()
            val base = FakePostingRepository().apply { challenge = challenge(byteArrayOf(9)) }
            var calls = 0
            val posting =
                object : PostingRepository by base {
                    override suspend fun selectEmoji(
                        challengeId: String,
                        emojiNumber: Int,
                    ): PostingOutcome<EmojiCaptchaStep> {
                        calls++
                        return response.await()
                    }
                }
            val fixture = StableCaptchaFixture(this, posting)
            fixture.session.load()
            runCurrent()
            val original = fixture.state.captcha
            fixture.session.select(0)
            runCurrent()
            fixture.session.select(1)
            assertSame(original, fixture.state.captcha)
            assertTrue(fixture.state.progress.captchaSelecting)
            assertEquals(1, calls)
            response.complete(PostingOutcome.Success(EmojiCaptchaStep.Solved("token")))
            advanceUntilIdle()
            assertTrue(fixture.state.captcha.satisfied)
            assertFalse(fixture.state.progress.captchaSelecting)
            assertTrue(
                fixture.state.captcha.keyboard
                    .isEmpty(),
            )
        }

    @Test fun changedTaskIsPublishedWhileReloadDiscardsPreviousLayoutEvenWithSameId() =
        runTest {
            val base =
                FakePostingRepository().apply {
                    challenge = challenge(byteArrayOf(9))
                    nextStep =
                        EmojiCaptchaStep.Challenge(byteArrayOf(8), listOf(option(0, 3), option(1, 2), option(2, 1)))
                }
            val fixture = StableCaptchaFixture(this, base)
            fixture.session.load()
            runCurrent()
            val first =
                fixture.state.captcha.challenge
                    ?.taskImage
            fixture.session.select(0)
            advanceUntilIdle()
            assertNotSame(
                first,
                fixture.state.captcha.challenge
                    ?.taskImage,
            )
            assertEquals(
                8.toByte(),
                fixture.state.captcha.challenge
                    ?.taskImage
                    ?.first(),
            )
            base.challenge = requireNotNull(base.challenge).copy(options = listOf(option(0, 3), option(1, 1)))
            fixture.session.load()
            assertTrue(
                fixture.state.captcha.keyboard
                    .isEmpty(),
            )
            runCurrent()
            assertEquals(
                listOf(3, 1),
                fixture.state.captcha.keyboard
                    .map { it?.image?.first()?.toInt() },
            )
            assertTrue(
                fixture.state.captcha.selectedImages
                    .isEmpty(),
            )
        }

    private fun challenge(task: ByteArray): EmojiCaptchaChallenge =
        EmojiCaptchaChallenge("same-id", task, listOf(option(0, 1), option(1, 2), option(2, 3)), 300)

    private fun option(
        number: Int,
        image: Int,
    ): EmojiCaptchaOption = EmojiCaptchaOption(number, byteArrayOf(image.toByte()))
}

@OptIn(ExperimentalCoroutinesApi::class)
private class StableCaptchaFixture(
    scope: TestScope,
    posting: PostingRepository,
) {
    var state = ComposerCaptchaSnapshot(ComposerCaptchaState(), ComposerProgressState(), ComposerFailureState(), null)
    val session =
        ComposerCaptchaSession(
            scope,
            posting,
            ComposerCaptchaContext(defaultBoardAddress()),
            ComposerCaptchaBindings({ state }, { state = it }, { error("Unexpected failure: $it") }),
            nowMillis = { 1_000L },
            preparationDispatcher = StandardTestDispatcher(scope.testScheduler),
        )
}
