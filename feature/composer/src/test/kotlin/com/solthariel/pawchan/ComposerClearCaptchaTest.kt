package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerAttachment
import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.NewThreadViewModel
import com.solthariel.pawchan.composer.ReplyComposerController
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import com.solthariel.pawchan.domain.posting.EmojiCaptchaStep
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComposerClearCaptchaTest {
    private val board = defaultBoardAddress()
    private val address = ThreadAddress(board, requireNotNull(ThreadId.parse(42)))

    private fun posting() =
        FakePostingRepository().apply {
            challenge =
                EmojiCaptchaChallenge(
                    "challenge",
                    byteArrayOf(1),
                    listOf(EmojiCaptchaOption(1, byteArrayOf(2))),
                )
        }

    private fun written(draft: ComposerDraftState) =
        draft.copy(
            comment = "text",
            email = "mail",
            subject = "subject",
            attachments = listOf(ComposerAttachment(ReplyAttachment("file.png", "image/png", byteArrayOf(1)))),
        )

    @Test fun replyClearKeepsSolvedCaptchaAndDoesNotReloadRulesOrChallenge() =
        runTest {
            val posting = posting()
            val controller = ReplyComposerController(this, address, posting, {})
            controller.open(false)
            advanceUntilIdle()
            controller.selectEmoji(1)
            advanceUntilIdle()
            controller.updateDraft(written(controller.state.value.draft))
            val captcha = controller.state.value.captchaState
            controller.clearAll()
            advanceUntilIdle()
            assertSame(captcha, controller.state.value.captchaState)
            assertEquals("token", controller.state.value.emojiCaptchaToken)
            assertEquals(1, posting.captchaLoads)
            assertEquals(1, posting.ruleLoads)
            assertEquals("", controller.state.value.comment)
            assertEquals("", controller.state.value.draft.email)
            assertTrue(
                controller.state.value.attachments
                    .isEmpty(),
            )
        }

    @Test fun newThreadClearKeepsSolvedCaptchaAndDoesNotReloadIt() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val posting = posting()
                val model = NewThreadViewModel(board, posting)
                advanceUntilIdle()
                model.selectEmoji(1)
                advanceUntilIdle()
                model.updateDraft(written(model.state.value.draft))
                val captcha = model.state.value.captchaState
                model.clearAll()
                advanceUntilIdle()
                assertSame(captcha, model.state.value.captchaState)
                assertEquals("token", model.state.value.emojiCaptchaToken)
                assertEquals(1, posting.captchaLoads)
                assertEquals(1, posting.ruleLoads)
                assertEquals("", model.state.value.subject)
                assertTrue(
                    model.state.value.attachments
                        .isEmpty(),
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun clearingDoesNotCancelAnEmojiSelectionAlreadyInFlight() =
        runTest {
            val posting = posting()
            val gate = CompletableDeferred<Unit>()
            posting.selectionGate = gate
            val controller = ReplyComposerController(this, address, posting, {})
            controller.open(false)
            advanceUntilIdle()
            controller.selectEmoji(1)
            runCurrent()
            val captcha = controller.state.value.captchaState
            controller.clearAll()
            assertTrue(controller.state.value.captchaSelecting)
            assertSame(captcha, controller.state.value.captchaState)
            gate.complete(Unit)
            advanceUntilIdle()
            assertEquals("token", controller.state.value.emojiCaptchaToken)
            assertEquals(1, posting.captchaLoads)
        }

    @Test fun clearingKeepsAPartiallySolvedChallengeAndSelectedImages() =
        runTest {
            val posting = posting()
            posting.nextStep = EmojiCaptchaStep.Challenge(byteArrayOf(3), listOf(EmojiCaptchaOption(2, byteArrayOf(4))))
            val controller = ReplyComposerController(this, address, posting, {})
            controller.open(false)
            advanceUntilIdle()
            controller.selectEmoji(1)
            advanceUntilIdle()
            val captcha = controller.state.value.captchaState
            controller.clearAll()
            advanceUntilIdle()
            assertSame(captcha, controller.state.value.captchaState)
            assertEquals(
                "challenge",
                controller.state.value.captcha
                    ?.id,
            )
            assertEquals(
                2,
                controller.state.value.captcha
                    ?.options
                    ?.single()
                    ?.number,
            )
            assertEquals(1, controller.state.value.selectedEmojiImages.size)
            assertEquals(1, posting.captchaLoads)
        }
}
